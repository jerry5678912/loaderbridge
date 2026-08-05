package dev.loaderbridge.fixture.attachment;

import com.mojang.serialization.Codec;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.ChunkAccess;

public final class FabricDataAttachmentFixture implements ModInitializer {
    private static final AttachmentType<Integer> PERSISTENT =
            AttachmentRegistry.createPersistent(id("persistent"), Codec.INT);
    private static final AttachmentType<Integer> DEFAULTED =
            AttachmentRegistry.createDefaulted(id("defaulted"), () -> 7);
    static final AttachmentType<Integer> SYNCED_LEVEL = AttachmentRegistry.create(
            id("synced_level"), builder -> builder.syncWith(
                    ByteBufCodecs.VAR_INT, (target, player) -> true));
    static final AttachmentType<Integer> SYNCED_PLAYER = AttachmentRegistry.create(
            id("synced_player"), builder -> builder.syncWith(
                    ByteBufCodecs.VAR_INT, (target, player) -> true));
    static final AttachmentType<Integer> SYNCED_ENTITY = AttachmentRegistry.create(
            id("synced_entity"), builder -> builder.persistent(Codec.INT).syncWith(
                    ByteBufCodecs.VAR_INT, (target, player) -> true));
    static final AttachmentType<Integer> SYNCED_BLOCK_ENTITY = AttachmentRegistry.create(
            id("synced_block_entity"), builder -> builder.syncWith(
                    ByteBufCodecs.VAR_INT, (target, player) -> true));
    static final AttachmentType<Integer> SYNCED_CHUNK = AttachmentRegistry.create(
            id("synced_chunk"), builder -> builder.syncWith(
                    ByteBufCodecs.VAR_INT, (target, player) -> true));
    private static final AtomicInteger JOIN_SESSIONS = new AtomicInteger();

    @Override public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> verify(server.overworld()));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ((AttachmentTarget) handler.player).setAttached(SYNCED_PLAYER, 59);
            if (JOIN_SESSIONS.incrementAndGet() == 1) {
                spawnSynchronizedEntity(handler.player.serverLevel());
            }
            System.out.println("LOADERBRIDGE_DATA_ATTACHMENT_SERVER_MUTATION_READY player=59");
        });
    }

    private static void verify(ServerLevel level) {
        AttachmentTarget levelTarget = (AttachmentTarget) level;
        levelTarget.setAttached(SYNCED_LEVEL, 53);
        BlockPos syncPos = level.getSharedSpawnPos().above(2);
        level.setBlockAndUpdate(syncPos, Blocks.CHEST.defaultBlockState());
        BlockEntity syncedBlockEntity = require(level.getBlockEntity(syncPos),
                "synchronized block entity");
        ((AttachmentTarget) syncedBlockEntity).setAttached(SYNCED_BLOCK_ENTITY, 71);
        ChunkAccess syncedChunk = level.getChunk(syncPos);
        ((AttachmentTarget) syncedChunk).setAttached(SYNCED_CHUNK, 73);
        if (levelTarget.getAttachedOrCreate(DEFAULTED) != 7
                || levelTarget.modifyAttached(DEFAULTED, value -> value + 1) != 7
                || levelTarget.getAttachedOrThrow(DEFAULTED) != 8) {
            throw new IllegalStateException("Data attachment level target failed");
        }

        Mob entity = require(EntityType.ZOMBIE.create(level), "entity");
        AttachmentTarget entityTarget = (AttachmentTarget) entity;
        entityTarget.setAttached(PERSISTENT, 19);
        CompoundTag entityTag = entity.saveWithoutId(new CompoundTag());
        Mob restoredEntity = require(EntityType.ZOMBIE.create(level), "restored entity");
        restoredEntity.load(entityTag);
        if (((AttachmentTarget) restoredEntity).getAttachedOrThrow(PERSISTENT) != 19) {
            throw new IllegalStateException("Data attachment entity persistence failed");
        }

        BlockPos pos = level.getSharedSpawnPos();
        BlockEntity blockEntity = require(BlockEntityType.CHEST.create(
                pos, Blocks.CHEST.defaultBlockState()), "block entity");
        ((AttachmentTarget) blockEntity).setAttached(PERSISTENT, 23);
        CompoundTag blockEntityTag = blockEntity.saveWithFullMetadata(level.registryAccess());
        BlockEntity restoredBlockEntity = require(BlockEntityType.CHEST.create(
                pos, Blocks.CHEST.defaultBlockState()), "restored block entity");
        restoredBlockEntity.loadWithComponents(blockEntityTag, level.registryAccess());
        if (((AttachmentTarget) restoredBlockEntity).getAttachedOrThrow(PERSISTENT) != 23) {
            throw new IllegalStateException("Data attachment block-entity persistence failed");
        }

        ChunkAccess chunk = level.getChunk(pos);
        AttachmentTarget chunkTarget = (AttachmentTarget) chunk;
        chunkTarget.setAttached(DEFAULTED, 31);
        if (!chunkTarget.hasAttached(DEFAULTED) || chunkTarget.getAttached(DEFAULTED) != 31
                || !chunk.isUnsaved()) {
            throw new IllegalStateException("Data attachment chunk target failed");
        }
        Integer savedLevel = levelTarget.getAttached(PERSISTENT);
        Integer savedChunk = chunkTarget.getAttached(PERSISTENT);
        if (savedLevel == null && savedChunk == null) {
            levelTarget.setAttached(PERSISTENT, 41);
            chunkTarget.setAttached(PERSISTENT, 43);
            System.out.println("LOADERBRIDGE_DATA_ATTACHMENT_PERSIST_INIT level=41 chunk=43");
        } else if (!Integer.valueOf(41).equals(savedLevel)
                || !Integer.valueOf(43).equals(savedChunk)) {
            throw new IllegalStateException("Data attachment process persistence failed: level="
                    + savedLevel + " chunk=" + savedChunk);
        } else {
            System.out.println("LOADERBRIDGE_DATA_ATTACHMENT_PERSIST_RELOAD level=41 chunk=43");
        }
        System.out.println("LOADERBRIDGE_DATA_ATTACHMENT_BASE_READY entity=19 block=23 "
                + "level=8 chunk=31");
    }

    private static void spawnSynchronizedEntity(ServerLevel level) {
        Mob entity = require(EntityType.COW.create(level), "synchronized entity");
        entity.setPos(level.getSharedSpawnPos().getX() + 2.5,
                level.getSharedSpawnPos().getY() + 1,
                level.getSharedSpawnPos().getZ() + 2.5);
        entity.setNoAi(true);
        entity.setInvulnerable(true);
        entity.setPersistenceRequired();
        ((AttachmentTarget) entity).setAttached(SYNCED_ENTITY, 67);
        if (!level.addFreshEntity(entity)) {
            throw new IllegalStateException("Could not add synchronized entity");
        }
        System.out.println("LOADERBRIDGE_DATA_ATTACHMENT_ENTITY_SPAWNED id=" + entity.getId());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("loaderbridge_fixture", path);
    }

    private static <T> T require(T value, String label) {
        if (value == null) throw new IllegalStateException("Could not create " + label);
        return value;
    }
}
