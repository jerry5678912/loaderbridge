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
import org.junit.jupiter.api.Test;

class TransactionContractTest {
    @Test
    void providerAdvertisesOnlyImplementedTransactionSurface() {
        var descriptor = new FabricTransferApiBridgeProvider().descriptor();
        assertThat(descriptor.implementationVersion())
                .isEqualTo("5.4.4+7b3d111d19-loaderbridge.1");
        assertThat(descriptor.providedClasses()).containsExactlyInAnyOrderElementsOf(Set.of(
                "net.fabricmc.fabric.api.transfer.v1.transaction.Transaction",
                "net.fabricmc.fabric.api.transfer.v1.transaction.Transaction$Lifecycle",
                "net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext",
                "net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext$CloseCallback",
                "net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext$OuterCloseCallback",
                "net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext$Result",
                "net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant"));
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
}
