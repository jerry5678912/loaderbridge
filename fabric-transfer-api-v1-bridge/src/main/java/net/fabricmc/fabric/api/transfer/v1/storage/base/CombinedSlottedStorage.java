package net.fabricmc.fabric.api.transfer.v1.storage.base;

import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;

/** Ordered composite retaining indexed slot access. */
public class CombinedSlottedStorage<T, S extends SlottedStorage<T>>
        extends CombinedStorage<T, S> implements SlottedStorage<T> {
    public CombinedSlottedStorage(List<S> parts) {
        super(parts);
    }

    @Override
    public int getSlotCount() {
        return parts.stream().mapToInt(SlottedStorage::getSlotCount).sum();
    }

    @Override
    public SingleSlotStorage<T> getSlot(int slot) {
        int remaining = slot;
        for (S part : parts) {
            if (remaining < part.getSlotCount()) return part.getSlot(remaining);
            remaining -= part.getSlotCount();
        }
        throw new IndexOutOfBoundsException(
                "Slot " + slot + " is out of bounds. This storage has size " + getSlotCount());
    }

    @Override
    public String toString() {
        return "CombinedSlottedStorage[" + String.join(", ",
                parts.stream().map(Object::toString).toList()) + "]";
    }
}
