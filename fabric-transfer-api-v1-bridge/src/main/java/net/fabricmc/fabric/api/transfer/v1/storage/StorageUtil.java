package net.fabricmc.fabric.api.transfer.v1.storage;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.util.Mth;

/** General transactional helpers for generic Fabric storages. */
public final class StorageUtil {
    private StorageUtil() {
    }

    public static <T> long move(Storage<T> from, Storage<T> to, Predicate<T> filter,
            long maxAmount, TransactionContext transaction) {
        Objects.requireNonNull(filter, "Filter may not be null");
        StoragePreconditions.notNegative(maxAmount);
        if (from == null || to == null) return 0;

        long totalMoved = 0;
        try (Transaction iteration = Transaction.openNested(transaction)) {
            for (StorageView<T> view : from.nonEmptyViews()) {
                T resource = view.getResource();
                if (!filter.test(resource)) continue;

                long extractable = simulateExtract(
                        view, resource, maxAmount - totalMoved, iteration);
                try (Transaction transfer = iteration.openNested()) {
                    long accepted = to.insert(resource, extractable, transfer);
                    if (view.extract(resource, accepted, transfer) == accepted) {
                        totalMoved += accepted;
                        transfer.commit();
                    }
                }

                if (totalMoved == maxAmount) {
                    iteration.commit();
                    return totalMoved;
                }
            }
            iteration.commit();
            return totalMoved;
        } catch (Exception exception) {
            throw storageFailure(exception, "Moving resources between storages",
                    "Move details", from, to, filter, maxAmount, transaction);
        }
    }

    public static <T> long simulateInsert(Storage<T> storage, T resource,
            long maxAmount, TransactionContext transaction) {
        try (Transaction simulation = Transaction.openNested(transaction)) {
            return storage.insert(resource, maxAmount, simulation);
        }
    }

    public static <T> long simulateExtract(Storage<T> storage, T resource,
            long maxAmount, TransactionContext transaction) {
        try (Transaction simulation = Transaction.openNested(transaction)) {
            return storage.extract(resource, maxAmount, simulation);
        }
    }

    public static <T> long simulateExtract(StorageView<T> view, T resource,
            long maxAmount, TransactionContext transaction) {
        try (Transaction simulation = Transaction.openNested(transaction)) {
            return view.extract(resource, maxAmount, simulation);
        }
    }

    public static <T, S extends Object & Storage<T> & StorageView<T>> long simulateExtract(
            S storage, T resource, long maxAmount, TransactionContext transaction) {
        try (Transaction simulation = Transaction.openNested(transaction)) {
            return storage.extract(resource, maxAmount, simulation);
        }
    }

    public static <T> ResourceAmount<T> extractAny(Storage<T> storage, long maxAmount,
            TransactionContext transaction) {
        StoragePreconditions.notNegative(maxAmount);
        if (storage == null) return null;
        try {
            for (StorageView<T> view : storage.nonEmptyViews()) {
                T resource = view.getResource();
                long amount = view.extract(resource, maxAmount, transaction);
                if (amount > 0) return new ResourceAmount<>(resource, amount);
            }
            return null;
        } catch (Exception exception) {
            CrashReport report = CrashReport.forThrowable(
                    exception, "Extracting resources from storage");
            CrashReportCategory category = report.addCategory("Extraction details");
            category.setDetail("Storage", storage::toString);
            category.setDetail("Max amount", maxAmount);
            category.setDetail("Transaction", transaction);
            throw new ReportedException(report);
        }
    }

    public static <T> long insertStacking(List<? extends SingleSlotStorage<T>> slots,
            T resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notNegative(maxAmount);
        long amount = 0;
        try {
            for (SingleSlotStorage<T> slot : slots) {
                if (!slot.isResourceBlank()) {
                    amount += slot.insert(resource, maxAmount - amount, transaction);
                    if (amount == maxAmount) return amount;
                }
            }
            for (SingleSlotStorage<T> slot : slots) {
                amount += slot.insert(resource, maxAmount - amount, transaction);
                if (amount == maxAmount) return amount;
            }
            return amount;
        } catch (Exception exception) {
            CrashReport report = CrashReport.forThrowable(
                    exception, "Inserting resources into slots");
            CrashReportCategory category = report.addCategory("Slotted insertion details");
            category.setDetail("Slots", () -> Objects.toString(slots, null));
            category.setDetail("Resource", () -> Objects.toString(resource, null));
            category.setDetail("Max amount", maxAmount);
            category.setDetail("Transaction", transaction);
            throw new ReportedException(report);
        }
    }

    public static <T> long tryInsertStacking(Storage<T> storage, T resource,
            long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notNegative(maxAmount);
        try {
            if (storage instanceof SlottedStorage<T> slotted) {
                return insertStacking(slotted.getSlots(), resource, maxAmount, transaction);
            }
            return storage == null ? 0 : storage.insert(resource, maxAmount, transaction);
        } catch (Exception exception) {
            CrashReport report = CrashReport.forThrowable(
                    exception, "Inserting resources into a storage");
            CrashReportCategory category = report.addCategory("Insertion details");
            category.setDetail("Storage", () -> Objects.toString(storage, null));
            category.setDetail("Resource", () -> Objects.toString(resource, null));
            category.setDetail("Max amount", maxAmount);
            category.setDetail("Transaction", transaction);
            throw new ReportedException(report);
        }
    }

    public static <T> T findStoredResource(Storage<T> storage) {
        return findStoredResource(storage, ignored -> true);
    }

    public static <T> T findStoredResource(Storage<T> storage, Predicate<T> filter) {
        Objects.requireNonNull(filter, "Filter may not be null");
        if (storage == null) return null;
        for (StorageView<T> view : storage.nonEmptyViews()) {
            T resource = view.getResource();
            if (filter.test(resource)) return resource;
        }
        return null;
    }

    public static <T> T findExtractableResource(Storage<T> storage,
            TransactionContext transaction) {
        return findExtractableResource(storage, ignored -> true, transaction);
    }

    public static <T> T findExtractableResource(Storage<T> storage, Predicate<T> filter,
            TransactionContext transaction) {
        Objects.requireNonNull(filter, "Filter may not be null");
        if (storage == null) return null;
        try (Transaction nested = Transaction.openNested(transaction)) {
            for (StorageView<T> view : storage.nonEmptyViews()) {
                T resource = view.getResource();
                if (filter.test(resource)
                        && view.extract(resource, Long.MAX_VALUE, nested) > 0) {
                    return resource;
                }
            }
        }
        return null;
    }

    public static <T> ResourceAmount<T> findExtractableContent(Storage<T> storage,
            TransactionContext transaction) {
        return findExtractableContent(storage, ignored -> true, transaction);
    }

    public static <T> ResourceAmount<T> findExtractableContent(Storage<T> storage,
            Predicate<T> filter, TransactionContext transaction) {
        T resource = findExtractableResource(storage, filter, transaction);
        if (resource == null) return null;
        long amount = simulateExtract(storage, resource, Long.MAX_VALUE, transaction);
        return amount > 0 ? new ResourceAmount<>(resource, amount) : null;
    }

    public static <T> int calculateComparatorOutput(Storage<T> storage) {
        if (storage == null) return 0;
        double fillPercentage = 0;
        int viewCount = 0;
        boolean hasNonEmptyView = false;
        for (StorageView<T> view : storage) {
            viewCount++;
            if (view.getAmount() > 0) {
                fillPercentage += (double) view.getAmount() / view.getCapacity();
                hasNonEmptyView = true;
            }
        }
        return Mth.floor(fillPercentage / viewCount * 14) + (hasNonEmptyView ? 1 : 0);
    }

    private static ReportedException storageFailure(Exception exception, String title,
            String categoryName, Object from, Object to, Object filter, long maxAmount,
            TransactionContext transaction) {
        CrashReport report = CrashReport.forThrowable(exception, title);
        CrashReportCategory category = report.addCategory(categoryName);
        category.setDetail("Input storage", () -> Objects.toString(from, null));
        category.setDetail("Output storage", () -> Objects.toString(to, null));
        category.setDetail("Filter", () -> Objects.toString(filter, null));
        category.setDetail("Max amount", maxAmount);
        category.setDetail("Transaction", transaction);
        return new ReportedException(report);
    }
}
