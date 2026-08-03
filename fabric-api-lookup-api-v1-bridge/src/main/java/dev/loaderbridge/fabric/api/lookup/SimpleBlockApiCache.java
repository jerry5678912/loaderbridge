package dev.loaderbridge.fabric.api.lookup;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A live semantic cache; later revisions may add Fabric's invalidation optimization. */
public final class SimpleBlockApiCache<A, C> implements BlockApiCache<A, C> {
    private final BlockApiLookup<A, C> lookup;
    private final ServerLevel world;
    private final BlockPos pos;

    public SimpleBlockApiCache(BlockApiLookup<A, C> lookup, ServerLevel world, BlockPos pos) {
        this.lookup = lookup;
        this.world = world;
        this.pos = pos;
    }

    @Override public A find(BlockState state, C context) {
        return lookup.find(world, pos, state, getBlockEntity(), context);
    }
    @Override public BlockEntity getBlockEntity() { return world.getBlockEntity(pos); }
    @Override public BlockApiLookup<A, C> getLookup() { return lookup; }
    @Override public ServerLevel getWorld() { return world; }
    @Override public BlockPos getPos() { return pos; }
}
