package dev.loaderbridge.fabric.api.transfer.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Player inventory and cursor implementations for the public transfer contract. */
public final class BridgePlayerInventoryStorage implements PlayerInventoryStorage {
    private static final Map<Inventory, BridgePlayerInventoryStorage> WRAPPERS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<AbstractContainerMenu, SingleSlotStorage<ItemVariant>> CURSORS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final Inventory inventory;
    private final InventoryStorage delegate;
    private final DroppedStacks droppedStacks = new DroppedStacks();

    private BridgePlayerInventoryStorage(Inventory inventory) {
        this.inventory = inventory;
        this.delegate = BridgeInventoryStorage.createGeneric(inventory, null);
    }

    public static PlayerInventoryStorage of(Inventory inventory) {
        synchronized (WRAPPERS) {
            return WRAPPERS.computeIfAbsent(inventory, BridgePlayerInventoryStorage::new);
        }
    }

    public static SingleSlotStorage<ItemVariant> cursor(AbstractContainerMenu menu) {
        synchronized (CURSORS) {
            return CURSORS.computeIfAbsent(menu, CursorSlot::new);
        }
    }

    @Override public List<SingleSlotStorage<ItemVariant>> getSlots() { return delegate.getSlots(); }
    @Override public boolean supportsInsertion() { return true; }
    @Override public boolean supportsExtraction() { return delegate.supportsExtraction(); }
    @Override public java.util.Iterator<StorageView<ItemVariant>> iterator() {
        return delegate.iterator();
    }
    @Override public long extract(ItemVariant resource, long maxAmount,
            TransactionContext transaction) {
        return delegate.extract(resource, maxAmount, transaction);
    }

    @Override
    public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        return offer(resource, maxAmount, transaction);
    }

    @Override
    public long offer(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        long remaining = maxAmount;
        for (InteractionHand hand : InteractionHand.values()) {
            SingleSlotStorage<ItemVariant> slot = getHandSlot(hand);
            if (slot.getResource().equals(resource)) {
                remaining -= slot.insert(resource, remaining, transaction);
                if (remaining == 0) return maxAmount;
            }
        }
        List<SingleSlotStorage<ItemVariant>> main = getSlots().subList(0, 36);
        remaining -= StorageUtil.insertStacking(main, resource, remaining, transaction);
        return maxAmount - remaining;
    }

    @Override
    public void drop(ItemVariant variant, long amount, boolean throwRandomly,
            boolean retainOwnership, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(variant, amount);
        if (amount > 0 && !inventory.player.level().isClientSide()) {
            droppedStacks.add(new Drop(variant, amount, throwRandomly, retainOwnership), transaction);
        }
    }

    @Override
    public SingleSlotStorage<ItemVariant> getHandSlot(InteractionHand hand) {
        return switch (hand) {
            case MAIN_HAND -> {
                if (!Inventory.isHotbarSlot(inventory.selected)) {
                    throw new IllegalStateException(
                            "Unexpected player selected slot: " + inventory.selected);
                }
                yield getSlot(inventory.selected);
            }
            case OFF_HAND -> getSlot(Inventory.SLOT_OFFHAND);
        };
    }

    private final class DroppedStacks extends SnapshotParticipant<Integer> {
        private final List<Drop> entries = new ArrayList<>();

        private void add(Drop drop, TransactionContext transaction) {
            updateSnapshots(transaction);
            entries.add(drop);
        }

        @Override protected Integer createSnapshot() { return entries.size(); }
        @Override protected void readSnapshot(Integer size) {
            while (entries.size() > size) entries.removeLast();
        }
        @Override protected void onFinalCommit() {
            for (Drop entry : entries) {
                long remaining = entry.amount();
                while (remaining > 0) {
                    int count = (int) Math.min(entry.variant().toStack().getMaxStackSize(), remaining);
                    inventory.player.drop(entry.variant().toStack(count),
                            entry.throwRandomly(), entry.retainOwnership());
                    remaining -= count;
                }
            }
            entries.clear();
        }
    }

    private record Drop(ItemVariant variant, long amount, boolean throwRandomly,
                        boolean retainOwnership) { }

    private static final class CursorSlot extends SingleStackStorage {
        private final AbstractContainerMenu menu;
        private CursorSlot(AbstractContainerMenu menu) { this.menu = menu; }
        @Override protected ItemStack getStack() { return menu.getCarried(); }
        @Override protected void setStack(ItemStack stack) { menu.setCarried(stack); }
    }
}
