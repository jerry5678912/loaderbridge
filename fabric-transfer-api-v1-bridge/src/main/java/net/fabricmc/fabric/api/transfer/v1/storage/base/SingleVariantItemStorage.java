package net.fabricmc.fabric.api.transfer.v1.storage.base;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.Item;

/** Fixed-capacity item-provided storage backed by the container item's components. */
public abstract class SingleVariantItemStorage<T extends TransferVariant<?>>
        implements SingleSlotStorage<T> {
    private final ContainerItemContext context;
    private final Item item;

    public SingleVariantItemStorage(ContainerItemContext context) {
        this.context = context;
        this.item = context.getItemVariant().getItem();
    }

    protected abstract T getBlankResource();

    protected abstract T getResource(ItemVariant currentVariant);

    protected abstract long getAmount(ItemVariant currentVariant);

    protected abstract long getCapacity(T variant);

    protected abstract ItemVariant getUpdatedVariant(
            ItemVariant currentVariant, T newResource, long newAmount);

    protected boolean canInsert(T resource) {
        return true;
    }

    protected boolean canExtract(T resource) {
        return true;
    }

    private boolean tryUpdateStorage(T newResource, long newAmount,
            TransactionContext transaction) {
        ItemVariant updated = getUpdatedVariant(
                context.getItemVariant(), newResource, newAmount);
        return context.exchange(updated, 1, transaction) == 1;
    }

    @Override
    public boolean supportsInsertion() {
        return context.getItemVariant().isOf(item);
    }

    @Override
    public long insert(T insertedResource, long maximum,
            TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(insertedResource, maximum);
        if (!canInsert(insertedResource) || !context.getItemVariant().isOf(item)) return 0;

        long amount = getAmount(context.getItemVariant());
        T resource = getResource(context.getItemVariant());
        long inserted = 0;
        if (resource.isBlank() || amount == 0) {
            inserted = Math.min(getCapacity(insertedResource), maximum);
        } else if (resource.equals(insertedResource)) {
            inserted = Math.min(getCapacity(insertedResource) - amount, maximum);
        }
        return inserted > 0 && tryUpdateStorage(insertedResource, amount + inserted, transaction)
                ? inserted : 0;
    }

    @Override
    public boolean supportsExtraction() {
        return context.getItemVariant().isOf(item);
    }

    @Override
    public long extract(T extractedResource, long maximum,
            TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(extractedResource, maximum);
        if (!canExtract(extractedResource) || !context.getItemVariant().isOf(item)) return 0;

        long amount = getAmount(context.getItemVariant());
        T resource = getResource(context.getItemVariant());
        long extracted = resource.equals(extractedResource) ? Math.min(maximum, amount) : 0;
        return extracted > 0 && tryUpdateStorage(resource, amount - extracted, transaction)
                ? extracted : 0;
    }

    @Override
    public boolean isResourceBlank() {
        return getResource().isBlank();
    }

    @Override
    public T getResource() {
        return context.getItemVariant().isOf(item)
                ? getResource(context.getItemVariant()) : getBlankResource();
    }

    @Override
    public long getAmount() {
        return context.getItemVariant().isOf(item)
                ? getAmount(context.getItemVariant()) : 0;
    }

    @Override
    public long getCapacity() {
        return context.getItemVariant().isOf(item) ? getCapacity(getResource()) : 0;
    }

    @Override
    public String toString() {
        return "SingleVariantItemStorage[" + context + '/' + item + ']';
    }
}
