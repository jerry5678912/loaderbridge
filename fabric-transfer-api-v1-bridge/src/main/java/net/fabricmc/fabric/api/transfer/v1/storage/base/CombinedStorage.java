package net.fabricmc.fabric.api.transfer.v1.storage.base;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.StreamSupport;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

/** Ordered composite of multiple storages. */
public class CombinedStorage<T, S extends Storage<T>> implements Storage<T> {
    public List<S> parts;

    public CombinedStorage(List<S> parts) {
        this.parts = parts;
    }

    @Override
    public boolean supportsInsertion() {
        return parts.stream().anyMatch(Storage::supportsInsertion);
    }

    @Override
    public long insert(T resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notNegative(maxAmount);
        long transferred = 0;
        for (S part : parts) {
            transferred += part.insert(resource, maxAmount - transferred, transaction);
            if (transferred == maxAmount) break;
        }
        return transferred;
    }

    @Override
    public boolean supportsExtraction() {
        return parts.stream().anyMatch(Storage::supportsExtraction);
    }

    @Override
    public long extract(T resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notNegative(maxAmount);
        long transferred = 0;
        for (S part : parts) {
            transferred += part.extract(resource, maxAmount - transferred, transaction);
            if (transferred == maxAmount) break;
        }
        return transferred;
    }

    @Override
    public Iterator<StorageView<T>> iterator() {
        return parts.stream().flatMap(part -> StreamSupport.stream(part.spliterator(), false))
                .iterator();
    }

    @Override
    public String toString() {
        return "CombinedStorage[" + String.join(", ",
                parts.stream().map(Object::toString).toList()) + "]";
    }
}
