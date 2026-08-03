package net.fabricmc.fabric.api.networking.v1;

import dev.loaderbridge.fabric.api.networking.mixin.ChunkMapAccessor;
import dev.loaderbridge.fabric.api.networking.mixin.TrackedEntityAccessor;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class PlayerLookup {
    public static Collection<ServerPlayer> all(MinecraftServer server) {
        Objects.requireNonNull(server, "The server cannot be null");
        return server.getPlayerList() == null ? Collections.emptyList()
                : Collections.unmodifiableCollection(server.getPlayerList().getPlayers());
    }

    public static Collection<ServerPlayer> world(ServerLevel world) {
        Objects.requireNonNull(world, "The world cannot be null");
        return Collections.unmodifiableCollection(world.players());
    }

    public static Collection<ServerPlayer> tracking(ServerLevel world, ChunkPos pos) {
        Objects.requireNonNull(world, "The world cannot be null");
        Objects.requireNonNull(pos, "The chunk pos cannot be null");
        return world.getChunkSource().chunkMap.getPlayers(pos, false);
    }

    public static Collection<ServerPlayer> tracking(Entity entity) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        if (!(entity.level() instanceof ServerLevel world)) {
            throw new IllegalArgumentException("Only supported on server worlds!");
        }
        var tracked = ((ChunkMapAccessor) world.getChunkSource().chunkMap)
                .loaderbridge$getEntityMap().get(entity.getId());
        if (tracked == null) return Collections.emptySet();
        return tracked.loaderbridge$getSeenBy().stream()
                .map(connection -> connection.getPlayer()).collect(Collectors.toUnmodifiableSet());
    }

    public static Collection<ServerPlayer> tracking(BlockEntity blockEntity) {
        Objects.requireNonNull(blockEntity, "BlockEntity cannot be null");
        if (!blockEntity.hasLevel() || blockEntity.getLevel().isClientSide()) {
            throw new IllegalArgumentException("Only supported on server worlds!");
        }
        return tracking((ServerLevel) blockEntity.getLevel(), blockEntity.getBlockPos());
    }

    public static Collection<ServerPlayer> tracking(ServerLevel world, BlockPos pos) {
        Objects.requireNonNull(pos, "BlockPos cannot be null");
        return tracking(world, new ChunkPos(pos));
    }

    public static Collection<ServerPlayer> around(ServerLevel world, Vec3 pos, double radius) {
        double radiusSquared = radius * radius;
        return world(world).stream().filter(player -> player.distanceToSqr(pos) <= radiusSquared)
                .collect(Collectors.toList());
    }

    public static Collection<ServerPlayer> around(ServerLevel world, Position pos, double radius) {
        double radiusSquared = radius * radius;
        return world(world).stream().filter(player -> player.distanceToSqr(
                pos.x(), pos.y(), pos.z()) <= radiusSquared).collect(Collectors.toList());
    }

    private PlayerLookup() { }
}
