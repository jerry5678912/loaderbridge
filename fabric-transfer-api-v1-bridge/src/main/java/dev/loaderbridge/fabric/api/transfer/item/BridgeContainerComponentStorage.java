package dev.loaderbridge.fabric.api.transfer.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedSlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Transactional item storage backed by a container data component. */
public final class BridgeContainerComponentStorage
        extends CombinedSlottedStorage<ItemVariant, SingleSlotStorage<ItemVariant>> {
    private final ContainerItemContext context;
    private final Item originalItem;

    public BridgeContainerComponentStorage(ContainerItemContext context, int slots) {
        super(Collections.emptyList());
        this.context = context;
        this.originalItem = context.getItemVariant().getItem();
        List<SingleSlotStorage<ItemVariant>> storageSlots = new ArrayList<>(slots);
        for (int slot = 0; slot < slots; slot++) storageSlots.add(new Slot(slot));
        parts = Collections.unmodifiableList(storageSlots);
    }

    private ItemContainerContents contents() {
        return context.getItemVariant().getComponentMap().getOrDefault(
                DataComponents.CONTAINER, ItemContainerContents.EMPTY);
    }

    private boolean isStillValid() {
        return context.getItemVariant().getItem() == originalItem;
    }

    private final class Slot implements SingleSlotStorage<ItemVariant> {
        private final int slot;

        private Slot(int slot) {
            this.slot = slot;
        }

        private ItemStack getStack() {
            List<ItemStack> stacks = contents().stream().toList();
            return stacks.size() > slot ? stacks.get(slot) : ItemStack.EMPTY;
        }

        private boolean setStack(ItemStack stack, TransactionContext transaction) {
            List<ItemStack> stacks = new ArrayList<>(
                    contents().stream().map(ItemStack::copy).toList());
            while (stacks.size() <= slot) stacks.add(ItemStack.EMPTY);
            stacks.set(slot, stack);
            ItemVariant updated = context.getItemVariant().withComponentChanges(
                    DataComponentPatch.builder().set(DataComponents.CONTAINER,
                            ItemContainerContents.fromItems(stacks)).build());
            return context.exchange(updated, 1, transaction) == 1;
        }

        @Override
        public long insert(ItemVariant resource, long maximum,
                TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maximum);
            if (!isStillValid() || !resource.getItem().canFitInsideContainerItems()) return 0;
            ItemStack current = getStack();
            if (!current.isEmpty() && !resource.matches(current)) return 0;
            long inserted = Math.min(maximum,
                    (long) resource.toStack().getMaxStackSize() - current.getCount());
            if (inserted <= 0) return 0;
            ItemStack updated = current.isEmpty()
                    ? resource.toStack((int) inserted) : current.copyWithCount(
                            current.getCount() + (int) inserted);
            return setStack(updated, transaction) ? inserted : 0;
        }

        @Override
        public long extract(ItemVariant resource, long maximum,
                TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maximum);
            if (!isStillValid()) return 0;
            ItemStack current = getStack();
            if (!resource.matches(current)) return 0;
            int extracted = (int) Math.min(maximum, current.getCount());
            if (extracted <= 0) return 0;
            ItemStack updated = current.copy();
            updated.shrink(extracted);
            return setStack(updated, transaction) ? extracted : 0;
        }

        @Override public boolean isResourceBlank() { return getStack().isEmpty(); }
        @Override public ItemVariant getResource() { return ItemVariant.of(getStack()); }
        @Override public long getAmount() { return getStack().getCount(); }
        @Override public long getCapacity() { return getStack().getMaxStackSize(); }

        @Override
        public String toString() {
            return "ContainerComponentSlot[" + context.getItemVariant() + '#' + slot + ']';
        }
    }
}
