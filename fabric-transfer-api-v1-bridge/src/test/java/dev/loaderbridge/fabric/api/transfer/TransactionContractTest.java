package dev.loaderbridge.fabric.api.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.BlankVariantView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedSlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import org.junit.jupiter.api.Test;

class TransactionContractTest {
    @Test
    void providerAdvertisesOnlyImplementedTransactionSurface() {
        var descriptor = new FabricTransferApiBridgeProvider().descriptor();
        assertThat(descriptor.implementationVersion())
                .isEqualTo("5.4.4+7b3d111d19-loaderbridge.10");
        assertThat(descriptor.providedClasses()).containsExactlyInAnyOrderElementsOf(Set.of(
                "net.fabricmc.fabric.api.transfer.v1.transaction.Transaction",
                "net.fabricmc.fabric.api.transfer.v1.transaction.Transaction$Lifecycle",
                "net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext",
                "net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext$CloseCallback",
                "net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext$OuterCloseCallback",
                "net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext$Result",
                "net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant",
                "net.fabricmc.fabric.api.transfer.v1.storage.Storage",
                "net.fabricmc.fabric.api.transfer.v1.storage.StorageView",
                "net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant",
                "net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions",
                "net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil",
                "net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage",
                "net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount",
                "net.fabricmc.fabric.api.transfer.v1.storage.base.BlankVariantView",
                "net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage",
                "net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage",
                "net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantItemStorage",
                "net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity",
                "net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage",
                "net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedSlottedStorage",
                "net.fabricmc.fabric.api.transfer.v1.storage.base.ExtractionOnlyStorage",
                "net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage",
                "net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage",
                "net.fabricmc.fabric.api.transfer.v1.item.ItemVariant",
                "net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage",
                "net.fabricmc.fabric.api.transfer.v1.item.ItemStorage",
                "net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage",
                "net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext",
                "net.fabricmc.fabric.api.transfer.v1.item.base.SingleItemStorage",
                "net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage",
                "net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants",
                "net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant",
                "net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage",
                "net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage$CombinedItemApiProvider",
                "net.fabricmc.fabric.api.transfer.v1.fluid.CauldronFluidContent",
                "net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil",
                "net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributeHandler",
                "net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes",
                "net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage",
                "net.fabricmc.fabric.api.transfer.v1.fluid.base.EmptyItemFluidStorage",
                "net.fabricmc.fabric.api.transfer.v1.fluid.base.FullItemFluidStorage"));
        assertThat(descriptor.requiredModules())
                .containsExactly("fabric-api-lookup-api-v1-bridge");
    }

    @Test
    void abortRollsBackSnapshotAndCommitFinalizesOnce() {
        CounterParticipant participant = new CounterParticipant();
        try (Transaction transaction = Transaction.openOuter()) {
            participant.set(10, transaction);
        }
        assertThat(participant.value).isZero();
        assertThat(participant.finalCommits).isZero();

        try (Transaction transaction = Transaction.openOuter()) {
            participant.set(20, transaction);
            transaction.commit();
        }
        assertThat(participant.value).isEqualTo(20);
        assertThat(participant.finalCommits).isEqualTo(1);
        assertThat(Transaction.getLifecycle()).isEqualTo(Transaction.Lifecycle.NONE);
    }

    @Test
    void nestedCommitStillRollsBackWithParentAndNestedAbortPreservesParent() {
        CounterParticipant participant = new CounterParticipant();
        try (Transaction outer = Transaction.openOuter()) {
            participant.set(1, outer);
            try (Transaction nested = outer.openNested()) {
                participant.set(2, nested);
                nested.commit();
            }
        }
        assertThat(participant.value).isZero();

        try (Transaction outer = Transaction.openOuter()) {
            participant.set(3, outer);
            try (Transaction nested = outer.openNested()) {
                participant.set(4, nested);
            }
            assertThat(participant.value).isEqualTo(3);
            outer.commit();
        }
        assertThat(participant.value).isEqualTo(3);
        assertThat(participant.finalCommits).isEqualTo(1);
    }

    @Test
    void callbacksAreLifoAndExposeClosingLifecycles() {
        List<String> calls = new ArrayList<>();
        try (Transaction transaction = Transaction.openOuter()) {
            transaction.addCloseCallback((context, result) ->
                    calls.add("first:" + Transaction.getLifecycle() + ":" + result));
            transaction.addCloseCallback((context, result) -> {
                calls.add("second:" + Transaction.getLifecycle() + ":" + result);
                context.addOuterCloseCallback(outerResult ->
                        calls.add("outer:" + Transaction.getLifecycle() + ":" + outerResult));
            });
            transaction.commit();
        }
        assertThat(calls).containsExactly(
                "second:CLOSING:COMMITTED",
                "first:CLOSING:COMMITTED",
                "outer:OUTER_CLOSING:COMMITTED");
    }

    @Test
    void transactionsRejectWrongThreadAndOutOfOrderClosure() throws Exception {
        try (Transaction outer = Transaction.openOuter()) {
            Transaction nested = outer.openNested();
            assertThatThrownBy(outer::commit).isInstanceOf(IllegalStateException.class);
            AtomicReference<Throwable> wrongThread = new AtomicReference<>();
            Thread thread = new Thread(() -> {
                try {
                    nested.nestingDepth();
                } catch (Throwable throwable) {
                    wrongThread.set(throwable);
                }
            }, "transfer-test-worker");
            thread.start();
            thread.join();
            assertThat(wrongThread.get()).isInstanceOf(IllegalStateException.class);
            nested.abort();
        }
    }

    @Test
    void emptyStorageAndNonEmptyIterationMatchContract() {
        Storage<String> empty = Storage.empty();
        assertThat(empty.supportsInsertion()).isFalse();
        assertThat(empty.supportsExtraction()).isFalse();
        assertThat(empty.iterator()).isExhausted();

        Storage<String> views = new Storage<>() {
            private final List<StorageView<String>> values = List.of(
                    view("", 0), view("energy", 4));

            @Override public long insert(String resource, long amount, TransactionContext tx) { return 0; }
            @Override public long extract(String resource, long amount, TransactionContext tx) { return 0; }
            @Override public java.util.Iterator<StorageView<String>> iterator() { return values.iterator(); }
        };
        assertThat(views.nonEmptyViews()).extracting(StorageView::getResource)
                .containsExactly("energy");
        try (Transaction transaction = Transaction.openOuter()) {
            assertThat(transaction.nestingDepth()).isZero();
            assertThatThrownBy(views::getVersion).isInstanceOf(IllegalStateException.class);
        }
        assertThat(Storage.<String>asClass()).isEqualTo(Storage.class);
    }

    @Test
    void combinedSlottedAndFilteringStoragesPreserveOrderAndRestrictions() {
        TestSlot first = new TestSlot(5, 10);
        TestSlot second = new TestSlot(0, 10);
        CombinedStorage<String, TestSlot> combined =
                new CombinedStorage<>(List.of(first, second));
        try (Transaction transaction = Transaction.openOuter()) {
            assertThat(combined.insert("energy", 9, transaction)).isEqualTo(9);
            transaction.commit();
        }
        assertThat(first.amount).isEqualTo(10);
        assertThat(second.amount).isEqualTo(4);

        CombinedSlottedStorage<String, TestSlot> slotted =
                new CombinedSlottedStorage<>(List.of(first, second));
        assertThat(slotted.getSlotCount()).isEqualTo(2);
        assertThat(slotted.getSlots()).containsExactly(first, second);
        assertThatThrownBy(() -> slotted.getSlot(2)).isInstanceOf(IndexOutOfBoundsException.class);

        Storage<String> readOnly = FilteringStorage.readOnlyOf(combined);
        try (Transaction transaction = Transaction.openOuter()) {
            assertThat(readOnly.insert("energy", 2, transaction)).isZero();
            assertThat(readOnly.iterator().next().extract("energy", 2, transaction)).isZero();
        }
        assertThat(readOnly.iterator().next().getUnderlyingView()).isSameAs(first);
    }

    @Test
    void storageUtilitiesMoveSimulateSearchAndStackTransactionally() {
        TestSlot source = new TestSlot(8, 10);
        TestSlot target = new TestSlot(0, 10);
        assertThat(StorageUtil.simulateExtract(source, "energy", 6, null)).isEqualTo(6);
        assertThat(source.amount).isEqualTo(8);
        assertThat(StorageUtil.simulateInsert(target, "energy", 6, null)).isEqualTo(6);
        assertThat(target.amount).isZero();

        assertThat(StorageUtil.move(source, target, "energy"::equals, 5, null)).isEqualTo(5);
        assertThat(source.amount).isEqualTo(3);
        assertThat(target.amount).isEqualTo(5);
        assertThat(StorageUtil.findStoredResource(target)).isEqualTo("energy");
        assertThat(StorageUtil.findStoredResource(target, "other"::equals)).isNull();
        assertThat(StorageUtil.findExtractableResource(target, null)).isEqualTo("energy");
        assertThat(StorageUtil.findExtractableContent(target, null))
                .isEqualTo(new ResourceAmount<>("energy", 5));
        assertThat(target.amount).isEqualTo(5);

        TestSlot empty = new TestSlot(0, 10);
        TestSlot partial = new TestSlot(7, 10);
        try (Transaction transaction = Transaction.openOuter()) {
            assertThat(StorageUtil.insertStacking(
                    List.of(empty, partial), "energy", 5, transaction)).isEqualTo(5);
            transaction.commit();
        }
        assertThat(partial.amount).isEqualTo(10);
        assertThat(empty.amount).isEqualTo(2);
        assertThat(StorageUtil.calculateComparatorOutput(
                new CombinedStorage<>(List.of(empty, partial)))).isEqualTo(9);

        try (Transaction transaction = Transaction.openOuter()) {
            assertThat(StorageUtil.extractAny(target, 2, transaction))
                    .isEqualTo(new ResourceAmount<>("energy", 2));
        }
        assertThat(target.amount).isEqualTo(5);
    }

    @Test
    void singleVariantStorageMaintainsVariantAmountAndSnapshots() {
        TestVariant blank = new TestVariant("", true);
        TestVariant energy = new TestVariant("energy", false);
        VariantSlot slot = new VariantSlot(blank, 10);
        assertThat(slot.getResource()).isEqualTo(blank);
        assertThat(slot.isResourceBlank()).isTrue();

        try (Transaction transaction = Transaction.openOuter()) {
            assertThat(slot.insert(energy, 7, transaction)).isEqualTo(7);
        }
        assertThat(slot.getResource()).isEqualTo(blank);
        assertThat(slot.getAmount()).isZero();

        try (Transaction transaction = Transaction.openOuter()) {
            assertThat(slot.insert(energy, 12, transaction)).isEqualTo(10);
            assertThat(slot.extract(energy, 3, transaction)).isEqualTo(3);
            transaction.commit();
        }
        assertThat(slot.getResource()).isEqualTo(energy);
        assertThat(slot.getAmount()).isEqualTo(7);
        assertThat(slot.finalCommits).isEqualTo(1);

        BlankVariantView<TestVariant> blankView = new BlankVariantView<>(blank, 40);
        assertThat(blankView.isResourceBlank()).isTrue();
        assertThat(blankView.getCapacity()).isEqualTo(40);
        assertThatThrownBy(() -> new BlankVariantView<>(energy, 40))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static StorageView<String> view(String resource, long amount) {
        return new StorageView<>() {
            @Override public long extract(String requested, long maximum, TransactionContext tx) { return 0; }
            @Override public boolean isResourceBlank() { return resource.isEmpty(); }
            @Override public String getResource() { return resource; }
            @Override public long getAmount() { return amount; }
            @Override public long getCapacity() { return 10; }
        };
    }

    private static final class CounterParticipant extends SnapshotParticipant<Integer> {
        private int value;
        private int finalCommits;

        private void set(int next, TransactionContext transaction) {
            updateSnapshots(transaction);
            value = next;
        }

        @Override
        protected Integer createSnapshot() {
            return value;
        }

        @Override
        protected void readSnapshot(Integer snapshot) {
            value = snapshot;
        }

        @Override
        protected void onFinalCommit() {
            finalCommits++;
        }
    }

    private static final class TestSlot extends SnapshotParticipant<Long>
            implements SingleSlotStorage<String> {
        private long amount;
        private final long capacity;

        private TestSlot(long amount, long capacity) {
            this.amount = amount;
            this.capacity = capacity;
        }

        @Override public long insert(String resource, long maximum, TransactionContext transaction) {
            long inserted = Math.min(maximum, capacity - amount);
            if (inserted > 0) { updateSnapshots(transaction); amount += inserted; }
            return inserted;
        }
        @Override public long extract(String resource, long maximum, TransactionContext transaction) {
            long extracted = Math.min(maximum, amount);
            if (extracted > 0) { updateSnapshots(transaction); amount -= extracted; }
            return extracted;
        }
        @Override public boolean isResourceBlank() { return amount == 0; }
        @Override public String getResource() { return "energy"; }
        @Override public long getAmount() { return amount; }
        @Override public long getCapacity() { return capacity; }
        @Override protected Long createSnapshot() { return amount; }
        @Override protected void readSnapshot(Long snapshot) { amount = snapshot; }
    }

    private record TestVariant(String object, boolean blank) implements TransferVariant<String> {
        @Override public boolean isBlank() { return blank; }
        @Override public String getObject() { return object; }
        @Override public DataComponentPatch getComponents() { return DataComponentPatch.EMPTY; }
        @Override public DataComponentMap getComponentMap() { return DataComponentMap.EMPTY; }
    }

    private static final class VariantSlot extends SingleVariantStorage<TestVariant> {
        private final TestVariant blank;
        private final long capacity;
        private int finalCommits;

        private VariantSlot(TestVariant blank, long capacity) {
            this.blank = blank;
            this.capacity = capacity;
            this.variant = blank;
        }

        @Override protected TestVariant getBlankVariant() {
            return blank;
        }

        @Override protected long getCapacity(TestVariant variant) {
            return capacity;
        }

        @Override protected void onFinalCommit() {
            finalCommits++;
        }
    }
}
