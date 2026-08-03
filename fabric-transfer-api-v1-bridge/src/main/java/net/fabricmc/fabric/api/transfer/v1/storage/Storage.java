package net.fabricmc.fabric.api.transfer.v1.storage;

import dev.loaderbridge.fabric.api.transfer.storage.BridgeStorageRuntime;
import java.util.Iterator;
import java.util.NoSuchElementException;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

/** Generic transactional resource storage. */
public interface Storage<T> extends Iterable<StorageView<T>> {
    static <T> Storage<T> empty() {
        return BridgeStorageRuntime.empty();
    }

    default boolean supportsInsertion() {
        return true;
    }

    long insert(T resource, long maxAmount, TransactionContext transaction);

    default boolean supportsExtraction() {
        return true;
    }

    long extract(T resource, long maxAmount, TransactionContext transaction);

    @Override
    Iterator<StorageView<T>> iterator();

    default Iterator<StorageView<T>> nonEmptyIterator() {
        Iterator<StorageView<T>> source = iterator();
        return new Iterator<>() {
            private StorageView<T> next;
            private boolean ready;

            @Override
            public boolean hasNext() {
                while (!ready && source.hasNext()) {
                    StorageView<T> candidate = source.next();
                    if (candidate.getAmount() > 0 && !candidate.isResourceBlank()) {
                        next = candidate;
                        ready = true;
                    }
                }
                return ready;
            }

            @Override
            public StorageView<T> next() {
                if (!hasNext()) throw new NoSuchElementException();
                StorageView<T> result = next;
                next = null;
                ready = false;
                return result;
            }
        };
    }

    default Iterable<StorageView<T>> nonEmptyViews() {
        return this::nonEmptyIterator;
    }

    default long getVersion() {
        if (Transaction.isOpen()) {
            throw new IllegalStateException("getVersion() may not be called during a transaction.");
        }
        return BridgeStorageRuntime.nextVersion();
    }

    @SuppressWarnings("unchecked")
    static <T> Class<Storage<T>> asClass() {
        return (Class<Storage<T>>) (Object) Storage.class;
    }
}
