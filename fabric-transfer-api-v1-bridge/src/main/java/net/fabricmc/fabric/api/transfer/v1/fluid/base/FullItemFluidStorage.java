package net.fabricmc.fabric.api.transfer.v1.fluid.base;

import java.util.Objects;
import java.util.function.Function;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ExtractionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.Item;

/** Converts one full fluid container into an empty item after exact extraction. */
public final class FullItemFluidStorage
        implements ExtractionOnlyStorage<FluidVariant>, SingleSlotStorage<FluidVariant> {
    private final ContainerItemContext context;
    private final Item fullItem;
    private final Function<ItemVariant, ItemVariant> fullToEmptyMapping;
    private final FluidVariant containedFluid;
    private final long containedAmount;

    public FullItemFluidStorage(ContainerItemContext context, Item emptyItem,
            FluidVariant containedFluid, long containedAmount) {
        this(context, full -> ItemVariant.of(emptyItem, full.getComponents()),
                containedFluid, containedAmount);
    }

    public FullItemFluidStorage(ContainerItemContext context,
            Function<ItemVariant, ItemVariant> fullToEmptyMapping,
            FluidVariant containedFluid, long containedAmount) {
        StoragePreconditions.notBlankNotNegative(containedFluid, containedAmount);
        this.context = Objects.requireNonNull(context, "Context may not be null");
        this.fullItem = context.getItemVariant().getItem();
        this.fullToEmptyMapping = Objects.requireNonNull(
                fullToEmptyMapping, "Mapping may not be null");
        this.containedFluid = containedFluid;
        this.containedAmount = containedAmount;
    }

    @Override public long extract(FluidVariant resource, long maximum,
            TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maximum);
        if (!context.getItemVariant().isOf(fullItem)) return 0;
        if (resource.equals(containedFluid) && maximum >= containedAmount) {
            ItemVariant empty = fullToEmptyMapping.apply(context.getItemVariant());
            if (context.exchange(empty, 1, transaction) == 1) return containedAmount;
        }
        return 0;
    }

    @Override public boolean isResourceBlank() { return getResource().isBlank(); }
    @Override public FluidVariant getResource() {
        return context.getItemVariant().isOf(fullItem) ? containedFluid : FluidVariant.blank();
    }
    @Override public long getAmount() {
        return context.getItemVariant().isOf(fullItem) ? containedAmount : 0;
    }
    @Override public long getCapacity() { return getAmount(); }

    @Override public String toString() {
        return "FullItemFluidStorage[context=%s, fluid=%s, amount=%d]"
                .formatted(context, containedFluid, containedAmount);
    }
}
