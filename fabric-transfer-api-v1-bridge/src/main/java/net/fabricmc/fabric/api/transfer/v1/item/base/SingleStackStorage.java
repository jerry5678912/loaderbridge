package net.fabricmc.fabric.api.transfer.v1.item.base;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.item.ItemStack;

/** Transactional single-slot storage backed by an ItemStack. */
public abstract class SingleStackStorage extends SnapshotParticipant<ItemStack>
        implements SingleSlotStorage<ItemVariant> {
    protected abstract ItemStack getStack();

    protected abstract void setStack(ItemStack stack);

    protected boolean canInsert(ItemVariant itemVariant) {
        return true;
    }

    protected boolean canExtract(ItemVariant itemVariant) {
        return true;
    }

    protected int getCapacity(ItemVariant itemVariant) {
        return itemVariant.getComponentMap().getOrDefault(
                net.minecraft.core.component.DataComponents.MAX_STACK_SIZE,
                itemVariant.getItem().getDefaultMaxStackSize());
    }

    @Override public boolean isResourceBlank() { return getStack().isEmpty(); }
    @Override public ItemVariant getResource() { return ItemVariant.of(getStack()); }
    @Override public long getAmount() { return getStack().getCount(); }
    @Override public long getCapacity() { return getCapacity(getResource()); }

    @Override
    public long insert(ItemVariant insertedVariant, long maxAmount,
            TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(insertedVariant, maxAmount);
        ItemStack current = getStack();
        if ((insertedVariant.matches(current) || current.isEmpty())
                && canInsert(insertedVariant)) {
            int inserted = (int) Math.min(maxAmount,
                    getCapacity(insertedVariant) - current.getCount());
            if (inserted > 0) {
                updateSnapshots(transaction);
                current = getStack();
                if (current.isEmpty()) current = insertedVariant.toStack(inserted);
                else current.grow(inserted);
                setStack(current);
                return inserted;
            }
        }
        return 0;
    }

    @Override
    public long extract(ItemVariant variant, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(variant, maxAmount);
        ItemStack current = getStack();
        if (variant.matches(current) && canExtract(variant)) {
            int extracted = (int) Math.min(current.getCount(), maxAmount);
            if (extracted > 0) {
                updateSnapshots(transaction);
                current = getStack();
                current.shrink(extracted);
                setStack(current);
                return extracted;
            }
        }
        return 0;
    }

    @Override
    protected ItemStack createSnapshot() {
        ItemStack original = getStack();
        setStack(original.copy());
        return original;
    }

    @Override protected void readSnapshot(ItemStack snapshot) { setStack(snapshot); }

    @Override public String toString() { return "SingleStackStorage[" + getStack() + ']'; }
}
