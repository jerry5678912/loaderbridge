package net.fabricmc.fabric.api.object.builder.v1.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.state.BlockState;

/** Supplies custom detector-rail comparator output for a minecart type. */
@FunctionalInterface
public interface MinecartComparatorLogic<T extends AbstractMinecart> {
    int getComparatorValue(T minecart, BlockState state, BlockPos position);
}
