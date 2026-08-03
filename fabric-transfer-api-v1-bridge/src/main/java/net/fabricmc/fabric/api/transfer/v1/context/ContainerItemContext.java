package net.fabricmc.fabric.api.transfer.v1.context;

import java.util.List;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Context through which item-provided storages exchange their containing item. */
public interface ContainerItemContext {
    static ContainerItemContext forPlayerInteraction(Player player, InteractionHand hand) {
        return player.isCreative()
                ? forCreativeInteraction(player, player.getItemInHand(hand))
                : ofPlayerHand(player, hand);
    }

    static ContainerItemContext forCreativeInteraction(Player player, ItemStack stack) {
        ContainerItemContext constant = withConstant(stack);
        PlayerInventoryStorage playerStorage = PlayerInventoryStorage.of(player);
        return new DelegatingContext(constant.getMainSlot(), playerStorage, true);
    }

    static ContainerItemContext ofPlayerHand(Player player, InteractionHand hand) {
        PlayerInventoryStorage storage = PlayerInventoryStorage.of(player);
        return new DelegatingContext(storage.getHandSlot(hand), storage, false);
    }

    static ContainerItemContext ofPlayerCursor(Player player, AbstractContainerMenu menu) {
        return ofPlayerSlot(player, PlayerInventoryStorage.getCursorStorage(menu));
    }

    static ContainerItemContext ofPlayerSlot(Player player,
            SingleSlotStorage<ItemVariant> slot) {
        return new DelegatingContext(slot, PlayerInventoryStorage.of(player), false);
    }

    static ContainerItemContext ofSingleSlot(SingleSlotStorage<ItemVariant> slot) {
        return new ContainerItemContext() {
            @Override public SingleSlotStorage<ItemVariant> getMainSlot() { return slot; }
            @Override public long insertOverflow(ItemVariant variant, long amount,
                    TransactionContext transaction) { return 0; }
            @Override public List<SingleSlotStorage<ItemVariant>> getAdditionalSlots() {
                return List.of();
            }
        };
    }

    static ContainerItemContext withConstant(ItemStack stack) {
        return withConstant(ItemVariant.of(stack), stack.getCount());
    }

    static ContainerItemContext withConstant(ItemVariant variant, long amount) {
        StoragePreconditions.notNegative(amount);
        ConstantSlot slot = new ConstantSlot(variant, amount);
        return new ContainerItemContext() {
            @Override public SingleSlotStorage<ItemVariant> getMainSlot() { return slot; }
            @Override public long insertOverflow(ItemVariant resource, long maximum,
                    TransactionContext transaction) {
                StoragePreconditions.notBlankNotNegative(resource, maximum);
                return maximum;
            }
            @Override public List<SingleSlotStorage<ItemVariant>> getAdditionalSlots() {
                return List.of();
            }
        };
    }

    default <A> A find(ItemApiLookup<A, ContainerItemContext> lookup) {
        return getItemVariant().isBlank() ? null
                : lookup.find(getItemVariant().toStack(), this);
    }

    default ItemVariant getItemVariant() {
        return getMainSlot().getResource();
    }

    default long getAmount() {
        if (getItemVariant().isBlank()) {
            throw new IllegalStateException(
                    "Amount may not be queried when the current item variant is blank.");
        }
        return getMainSlot().getAmount();
    }

    default long insert(ItemVariant variant, long maxAmount, TransactionContext transaction) {
        long main = getMainSlot().insert(variant, maxAmount, transaction);
        return main + insertOverflow(variant, maxAmount - main, transaction);
    }

    default long extract(ItemVariant variant, long maxAmount, TransactionContext transaction) {
        return getMainSlot().extract(variant, maxAmount, transaction);
    }

    default long exchange(ItemVariant newVariant, long maxAmount,
            TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(newVariant, maxAmount);
        try (Transaction nested = transaction.openNested()) {
            long extracted = extract(getItemVariant(), maxAmount, nested);
            if (insert(newVariant, extracted, nested) == extracted) {
                nested.commit();
                return extracted;
            }
        }
        return 0;
    }

    SingleSlotStorage<ItemVariant> getMainSlot();

    long insertOverflow(ItemVariant variant, long maxAmount, TransactionContext transaction);

    List<SingleSlotStorage<ItemVariant>> getAdditionalSlots();

    final class ConstantSlot implements SingleSlotStorage<ItemVariant> {
        private final ItemVariant variant;
        private final long amount;

        private ConstantSlot(ItemVariant variant, long amount) {
            this.variant = variant;
            this.amount = amount;
        }

        @Override public long insert(ItemVariant resource, long maximum,
                TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maximum);
            return 0;
        }
        @Override public long extract(ItemVariant resource, long maximum,
                TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maximum);
            return maximum;
        }
        @Override public boolean isResourceBlank() { return variant.isBlank(); }
        @Override public ItemVariant getResource() { return variant; }
        @Override public long getAmount() { return amount; }
        @Override public long getCapacity() { return Long.MAX_VALUE; }
    }

    final class DelegatingContext implements ContainerItemContext {
        private final SingleSlotStorage<ItemVariant> main;
        private final PlayerInventoryStorage playerStorage;
        private final boolean creative;

        private DelegatingContext(SingleSlotStorage<ItemVariant> main,
                PlayerInventoryStorage playerStorage, boolean creative) {
            this.main = main;
            this.playerStorage = playerStorage;
            this.creative = creative;
        }

        @Override public SingleSlotStorage<ItemVariant> getMainSlot() { return main; }

        @Override
        public long insertOverflow(ItemVariant variant, long maximum,
                TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(variant, maximum);
            if (creative) {
                boolean present = playerStorage.getSlots().stream()
                        .anyMatch(slot -> slot.getAmount() > 0
                                && slot.getResource().equals(variant));
                if (!present && maximum > 0) playerStorage.offer(variant, 1, transaction);
                return maximum;
            }
            playerStorage.offerOrDrop(variant, maximum, transaction);
            return maximum;
        }

        @Override public List<SingleSlotStorage<ItemVariant>> getAdditionalSlots() {
            return playerStorage.getSlots();
        }
    }
}
