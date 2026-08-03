package dev.loaderbridge.fabric.api.transfer.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;

/** Vanilla-container implementation used by InventoryStorage.of. */
public final class BridgeInventoryStorage
        extends CombinedStorage<ItemVariant, SingleSlotStorage<ItemVariant>>
        implements InventoryStorage {
    private static final Map<Container, BridgeInventoryStorage> WRAPPERS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final Container inventory;
    private final Direction direction;

    private BridgeInventoryStorage(Container inventory, Direction direction, List<Integer> slots) {
        super(buildSlots(inventory, direction, slots));
        this.inventory = inventory;
        this.direction = direction;
    }

    public static InventoryStorage of(Container inventory, Direction direction) {
        if (inventory instanceof Inventory playerInventory && direction == null) {
            return BridgePlayerInventoryStorage.of(playerInventory);
        }
        return createGeneric(inventory, direction);
    }

    static BridgeInventoryStorage createGeneric(Container inventory, Direction direction) {
        if (direction == null || !(inventory instanceof WorldlyContainer)) {
            synchronized (WRAPPERS) {
                BridgeInventoryStorage existing = WRAPPERS.get(inventory);
                if (existing != null && existing.parts.size() == inventory.getContainerSize()) {
                    return existing;
                }
                List<Integer> slots = new ArrayList<>(inventory.getContainerSize());
                for (int slot = 0; slot < inventory.getContainerSize(); slot++) slots.add(slot);
                BridgeInventoryStorage created =
                        new BridgeInventoryStorage(inventory, null, slots);
                WRAPPERS.put(inventory, created);
                return created;
            }
        }
        int[] accessible = ((WorldlyContainer) inventory).getSlotsForFace(direction);
        return new BridgeInventoryStorage(inventory, direction,
                Arrays.stream(accessible).boxed().toList());
    }

    private static List<SingleSlotStorage<ItemVariant>> buildSlots(
            Container inventory, Direction direction, List<Integer> slots) {
        List<SingleSlotStorage<ItemVariant>> wrappers = new ArrayList<>(slots.size());
        for (int slot : slots) wrappers.add(new Slot(inventory, direction, slot));
        return Collections.unmodifiableList(wrappers);
    }

    @Override
    public List<SingleSlotStorage<ItemVariant>> getSlots() {
        return parts;
    }

    @Override
    public String toString() {
        return "InventoryStorage[" + inventory + ", side=" + direction + ']';
    }

    private static final class Slot extends SingleStackStorage {
        private final Container inventory;
        private final Direction direction;
        private final int slot;

        private Slot(Container inventory, Direction direction, int slot) {
            this.inventory = inventory;
            this.direction = direction;
            this.slot = slot;
        }

        @Override protected ItemStack getStack() { return inventory.getItem(slot); }
        @Override protected void setStack(ItemStack stack) { inventory.setItem(slot, stack); }

        @Override
        protected boolean canInsert(ItemVariant variant) {
            ItemStack stack = variant.toStack();
            if (!inventory.canPlaceItem(slot, stack)) return false;
            return !(inventory instanceof WorldlyContainer sided) || direction == null
                    || sided.canPlaceItemThroughFace(slot, stack, direction);
        }

        @Override
        protected boolean canExtract(ItemVariant variant) {
            return !(inventory instanceof WorldlyContainer sided) || direction == null
                    || sided.canTakeItemThroughFace(slot, getStack(), direction);
        }

        @Override
        protected int getCapacity(ItemVariant variant) {
            return Math.min(inventory.getMaxStackSize(variant.toStack()),
                    super.getCapacity(variant));
        }

        @Override
        public void updateSnapshots(TransactionContext transaction) {
            super.updateSnapshots(transaction);
        }

        @Override
        protected void onFinalCommit() {
            inventory.setChanged();
        }

        @Override
        public String toString() {
            return "InventorySlotStorage[" + inventory + '#' + slot + ']';
        }
    }
}
