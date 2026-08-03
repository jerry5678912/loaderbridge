package net.fabricmc.fabric.api.transfer.v1.storage.base;

import java.util.Iterator;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

/** Delegating storage with insertion and extraction predicates. */
public abstract class FilteringStorage<T> implements Storage<T> {
    protected final Supplier<Storage<T>> backingStorage;

    public FilteringStorage(Storage<T> backingStorage) {
        this(() -> backingStorage);
    }

    public FilteringStorage(Supplier<Storage<T>> backingStorage) {
        this.backingStorage = backingStorage;
    }

    public static <T> Storage<T> insertOnlyOf(Storage<T> backing) {
        return of(backing, true, false);
    }

    public static <T> Storage<T> extractOnlyOf(Storage<T> backing) {
        return of(backing, false, true);
    }

    public static <T> Storage<T> readOnlyOf(Storage<T> backing) {
        return of(backing, false, false);
    }

    public static <T> Storage<T> of(Storage<T> backing, boolean insert, boolean extract) {
        if (insert && extract) return backing;
        return new FilteringStorage<>(backing) {
            @Override protected boolean canInsert(T resource) { return insert; }
            @Override protected boolean canExtract(T resource) { return extract; }
            @Override public boolean supportsInsertion() { return insert && super.supportsInsertion(); }
            @Override public boolean supportsExtraction() { return extract && super.supportsExtraction(); }
        };
    }

    protected boolean canInsert(T resource) { return true; }
    protected boolean canExtract(T resource) { return true; }

    @Override public boolean supportsInsertion() { return backingStorage.get().supportsInsertion(); }
    @Override public boolean supportsExtraction() { return backingStorage.get().supportsExtraction(); }

    @Override
    public long insert(T resource, long maximum, TransactionContext transaction) {
        return canInsert(resource) ? backingStorage.get().insert(resource, maximum, transaction) : 0;
    }

    @Override
    public long extract(T resource, long maximum, TransactionContext transaction) {
        return canExtract(resource) ? backingStorage.get().extract(resource, maximum, transaction) : 0;
    }

    @Override
    public Iterator<StorageView<T>> iterator() {
        Iterator<StorageView<T>> source = backingStorage.get().iterator();
        return new Iterator<>() {
            @Override public boolean hasNext() { return source.hasNext(); }
            @Override public StorageView<T> next() { return new FilteringView(source.next()); }
        };
    }

    @Override public long getVersion() { return backingStorage.get().getVersion(); }

    private final class FilteringView implements StorageView<T> {
        private final StorageView<T> backing;
        private FilteringView(StorageView<T> backing) { this.backing = backing; }
        @Override public long extract(T resource, long maximum, TransactionContext transaction) {
            return canExtract(resource) ? backing.extract(resource, maximum, transaction) : 0;
        }
        @Override public boolean isResourceBlank() { return backing.isResourceBlank(); }
        @Override public T getResource() { return backing.getResource(); }
        @Override public long getAmount() { return backing.getAmount(); }
        @Override public long getCapacity() { return backing.getCapacity(); }
        @Override public StorageView<T> getUnderlyingView() { return backing.getUnderlyingView(); }
    }
}
