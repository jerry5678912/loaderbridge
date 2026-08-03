package dev.loaderbridge.fabric.api.transfer.storage;

import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

/** Shared immutable empty storage and monotonic fallback version source. */
public final class BridgeStorageRuntime {
    private static final AtomicLong VERSIONS = new AtomicLong();
    private static final Storage<Object> EMPTY = new Storage<>() {
        @Override
        public boolean supportsInsertion() {
            return false;
        }

        @Override
        public long insert(Object resource, long maxAmount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public boolean supportsExtraction() {
            return false;
        }

        @Override
        public long extract(Object resource, long maxAmount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public Iterator<StorageView<Object>> iterator() {
            return Collections.emptyIterator();
        }
    };

    private BridgeStorageRuntime() {
    }

    @SuppressWarnings("unchecked")
    public static <T> Storage<T> empty() {
        return (Storage<T>) EMPTY;
    }

    public static long nextVersion() {
        return VERSIONS.getAndIncrement();
    }
}
