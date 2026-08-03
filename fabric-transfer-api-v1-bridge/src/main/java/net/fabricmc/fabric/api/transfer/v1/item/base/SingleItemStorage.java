package net.fabricmc.fabric.api.transfer.v1.item.base;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/** Item specialization of single-variant storage. */
public abstract class SingleItemStorage extends SingleVariantStorage<ItemVariant> {
    @Override
    protected final ItemVariant getBlankVariant() {
        return ItemVariant.blank();
    }

    public void readNbt(CompoundTag nbt, HolderLookup.Provider wrapperLookup) {
        SingleVariantStorage.readNbt(
                this, ItemVariant.CODEC, ItemVariant::blank, nbt, wrapperLookup);
    }

    public void writeNbt(CompoundTag nbt, HolderLookup.Provider wrapperLookup) {
        SingleVariantStorage.writeNbt(this, ItemVariant.CODEC, nbt, wrapperLookup);
    }
}
