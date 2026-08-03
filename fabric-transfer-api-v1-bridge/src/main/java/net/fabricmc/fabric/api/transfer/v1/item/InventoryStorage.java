package net.fabricmc.fabric.api.transfer.v1.item;

import dev.loaderbridge.fabric.api.transfer.item.BridgeInventoryStorage;
import java.util.List;
import java.util.Objects;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;

/** Transactional item-storage view over a vanilla Container. */
public interface InventoryStorage extends SlottedStorage<ItemVariant> {
    static InventoryStorage of(Container inventory, Direction direction) {
        Objects.requireNonNull(inventory, "Null inventory is not supported.");
        return BridgeInventoryStorage.of(inventory, direction);
    }

    @Override
    List<SingleSlotStorage<ItemVariant>> getSlots();

    @Override
    default int getSlotCount() {
        return getSlots().size();
    }

    @Override
    default SingleSlotStorage<ItemVariant> getSlot(int slot) {
        return getSlots().get(slot);
    }
}
