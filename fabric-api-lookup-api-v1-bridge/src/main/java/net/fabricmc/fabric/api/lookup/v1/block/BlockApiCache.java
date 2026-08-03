package net.fabricmc.fabric.api.lookup.v1.block;

import dev.loaderbridge.fabric.api.lookup.SimpleBlockApiCache;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Binary-compatible cache facade with semantically correct live queries. */
public interface BlockApiCache<A, C> {
    default A find(C context) {
        return find(null, context);
    }

    A find(BlockState state, C context);

    BlockEntity getBlockEntity();

    BlockApiLookup<A, C> getLookup();

    ServerLevel getWorld();

    BlockPos getPos();

    static <A, C> BlockApiCache<A, C> create(BlockApiLookup<A, C> lookup,
            ServerLevel world, BlockPos pos) {
        Objects.requireNonNull(lookup, "BlockApiLookup may not be null.");
        Objects.requireNonNull(pos, "BlockPos may not be null.");
        Objects.requireNonNull(world, "ServerWorld may not be null.");
        return new SimpleBlockApiCache<>(lookup, world, pos.immutable());
    }
}
