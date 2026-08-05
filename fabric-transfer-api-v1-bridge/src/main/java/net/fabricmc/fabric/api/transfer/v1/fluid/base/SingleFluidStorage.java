package net.fabricmc.fabric.api.transfer.v1.fluid.base;

import java.util.Objects;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/** Snapshot-backed single-fluid storage with Fabric-compatible NBT helpers. */
public abstract class SingleFluidStorage extends SingleVariantStorage<FluidVariant> {
    public static SingleFluidStorage withFixedCapacity(long capacity, Runnable onChange) {
        StoragePreconditions.notNegative(capacity);
        Objects.requireNonNull(onChange, "onChange may not be null");
        return new SingleFluidStorage() {
            @Override protected long getCapacity(FluidVariant variant) { return capacity; }
            @Override protected void onFinalCommit() { onChange.run(); }
        };
    }

    @Override protected final FluidVariant getBlankVariant() { return FluidVariant.blank(); }

    public void readNbt(CompoundTag nbt, HolderLookup.Provider wrapperLookup) {
        SingleVariantStorage.readNbt(
                this, FluidVariant.CODEC, FluidVariant::blank, nbt, wrapperLookup);
    }

    public void writeNbt(CompoundTag nbt, HolderLookup.Provider wrapperLookup) {
        SingleVariantStorage.writeNbt(this, FluidVariant.CODEC, nbt, wrapperLookup);
    }
}
