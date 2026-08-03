package net.fabricmc.fabric.api.transfer.v1.storage.base;

import java.util.Collections;
import java.util.Iterator;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;

/** Storage that is also its only view. */
public interface SingleSlotStorage<T> extends SlottedStorage<T>, StorageView<T> {
    @Override
    default Iterator<StorageView<T>> iterator() {
        return Collections.<StorageView<T>>singleton(this).iterator();
    }

    @Override
    default int getSlotCount() {
        return 1;
    }

    @Override
    default SingleSlotStorage<T> getSlot(int slot) {
        if (slot != 0) {
            throw new IndexOutOfBoundsException(
                    "Slot " + slot + " does not exist in a single-slot storage.");
        }
        return this;
    }
}
