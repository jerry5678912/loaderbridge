package net.fabricmc.fabric.impl.attachment.sync;

import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;

public sealed interface AttachmentTargetInfo permits AttachmentTargetInfo.BlockEntityTarget,
        AttachmentTargetInfo.EntityTarget, AttachmentTargetInfo.ChunkTarget,
        AttachmentTargetInfo.LevelTarget {
    byte BLOCK_ENTITY = 0;
    byte ENTITY = 1;
    byte CHUNK = 2;
    byte LEVEL = 3;

    void encode(RegistryFriendlyByteBuf buffer);
    AttachmentTarget resolve(Level level);

    static AttachmentTargetInfo of(AttachmentTarget target) {
        Object value = target;
        if (value instanceof BlockEntity blockEntity) {
            return new BlockEntityTarget(blockEntity.getBlockPos());
        }
        if (value instanceof Entity entity) return new EntityTarget(entity.getId());
        if (value instanceof ChunkAccess chunk) return new ChunkTarget(chunk.getPos());
        if (value instanceof Level) return LevelTarget.INSTANCE;
        return null;
    }

    static AttachmentTargetInfo decode(RegistryFriendlyByteBuf buffer) {
        return switch (buffer.readByte()) {
            case BLOCK_ENTITY -> new BlockEntityTarget(buffer.readBlockPos());
            case ENTITY -> new EntityTarget(buffer.readVarInt());
            case CHUNK -> new ChunkTarget(buffer.readChunkPos());
            case LEVEL -> LevelTarget.INSTANCE;
            default -> throw new IllegalArgumentException("LB-ATTACH-001: unknown target type");
        };
    }

    record BlockEntityTarget(BlockPos position) implements AttachmentTargetInfo {
        @Override public void encode(RegistryFriendlyByteBuf buffer) {
            buffer.writeByte(BLOCK_ENTITY).writeBlockPos(position);
        }
        @Override public AttachmentTarget resolve(Level level) {
            return (AttachmentTarget) (Object) level.getBlockEntity(position);
        }
    }

    record EntityTarget(int networkId) implements AttachmentTargetInfo {
        @Override public void encode(RegistryFriendlyByteBuf buffer) {
            buffer.writeByte(ENTITY).writeVarInt(networkId);
        }
        @Override public AttachmentTarget resolve(Level level) {
            return (AttachmentTarget) (Object) level.getEntity(networkId);
        }
    }

    record ChunkTarget(ChunkPos position) implements AttachmentTargetInfo {
        @Override public void encode(RegistryFriendlyByteBuf buffer) {
            buffer.writeByte(CHUNK).writeChunkPos(position);
        }
        @Override public AttachmentTarget resolve(Level level) {
            return (AttachmentTarget) (Object) level.getChunkSource()
                    .getChunkNow(position.x, position.z);
        }
    }

    enum LevelTarget implements AttachmentTargetInfo {
        INSTANCE;
        @Override public void encode(RegistryFriendlyByteBuf buffer) { buffer.writeByte(LEVEL); }
        @Override public AttachmentTarget resolve(Level level) {
            return (AttachmentTarget) (Object) level;
        }
    }
}
