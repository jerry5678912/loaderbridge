package net.fabricmc.fabric.api.transfer.v1.fluid.base;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.BlankVariantView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

/** Converts one empty container item into a full item after an exact fluid fill. */
public final class EmptyItemFluidStorage implements InsertionOnlyStorage<FluidVariant> {
    private final ContainerItemContext context;
    private final Item emptyItem;
    private final Function<ItemVariant, ItemVariant> emptyToFullMapping;
    private final Fluid insertableFluid;
    private final long insertableAmount;
    private final List<StorageView<FluidVariant>> blankView;

    public EmptyItemFluidStorage(ContainerItemContext context, Item fullItem,
            Fluid insertableFluid, long insertableAmount) {
        this(context, empty -> ItemVariant.of(fullItem, empty.getComponents()),
                insertableFluid, insertableAmount);
    }

    public EmptyItemFluidStorage(ContainerItemContext context,
            Function<ItemVariant, ItemVariant> emptyToFullMapping,
            Fluid insertableFluid, long insertableAmount) {
        StoragePreconditions.notNegative(insertableAmount);
        this.context = Objects.requireNonNull(context, "Context may not be null");
        this.emptyItem = context.getItemVariant().getItem();
        this.emptyToFullMapping = Objects.requireNonNull(
                emptyToFullMapping, "Mapping may not be null");
        this.insertableFluid = Objects.requireNonNull(
                insertableFluid, "Fluid may not be null");
        this.insertableAmount = insertableAmount;
        this.blankView = List.of(new BlankVariantView<>(FluidVariant.blank(), insertableAmount));
    }

    @Override public long insert(FluidVariant resource, long maximum,
            TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maximum);
        if (!context.getItemVariant().isOf(emptyItem)) return 0;
        if (resource.isOf(insertableFluid) && maximum >= insertableAmount) {
            ItemVariant full = emptyToFullMapping.apply(context.getItemVariant());
            if (context.exchange(full, 1, transaction) == 1) return insertableAmount;
        }
        return 0;
    }

    @Override public Iterator<StorageView<FluidVariant>> iterator() {
        return blankView.iterator();
    }

    @Override public String toString() {
        return "EmptyItemFluidStorage[context=%s, insertableFluid=%s, insertableAmount=%d]"
                .formatted(context, insertableFluid, insertableAmount);
    }
}
