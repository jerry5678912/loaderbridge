package net.fabricmc.fabric.api.transfer.v1.storage.base;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.Direction;

/** Optional block-entity helper for Fabric's standard sided storage lookups. */
public interface SidedStorageBlockEntity {
    default Storage<FluidVariant> getFluidStorage(Direction side) { return null; }
    default Storage<ItemVariant> getItemStorage(Direction side) { return null; }
}
