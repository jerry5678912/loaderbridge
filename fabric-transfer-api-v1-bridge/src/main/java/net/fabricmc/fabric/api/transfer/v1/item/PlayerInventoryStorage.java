package net.fabricmc.fabric.api.transfer.v1.item;

import dev.loaderbridge.fabric.api.transfer.item.BridgePlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Transactional item storage specialized for player inventory behavior. */
public interface PlayerInventoryStorage extends InventoryStorage {
    static PlayerInventoryStorage of(Player player) {
        return of(player.getInventory());
    }

    static PlayerInventoryStorage of(Inventory inventory) {
        return BridgePlayerInventoryStorage.of(inventory);
    }

    static SingleSlotStorage<ItemVariant> getCursorStorage(AbstractContainerMenu menu) {
        return BridgePlayerInventoryStorage.cursor(menu);
    }

    @Override
    long insert(ItemVariant resource, long maxAmount, TransactionContext transaction);

    default void offerOrDrop(ItemVariant variant, long amount, TransactionContext transaction) {
        long offered = offer(variant, amount, transaction);
        drop(variant, amount - offered, transaction);
    }

    long offer(ItemVariant variant, long maxAmount, TransactionContext transaction);

    void drop(ItemVariant variant, long amount, boolean throwRandomly,
            boolean retainOwnership, TransactionContext transaction);

    default void drop(ItemVariant variant, long amount, boolean retainOwnership,
            TransactionContext transaction) {
        drop(variant, amount, false, retainOwnership, transaction);
    }

    default void drop(ItemVariant variant, long amount, TransactionContext transaction) {
        drop(variant, amount, false, transaction);
    }

    SingleSlotStorage<ItemVariant> getHandSlot(InteractionHand hand);
}
