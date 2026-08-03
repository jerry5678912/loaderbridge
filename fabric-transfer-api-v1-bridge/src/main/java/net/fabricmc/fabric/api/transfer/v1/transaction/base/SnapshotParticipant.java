package net.fabricmc.fabric.api.transfer.v1.transaction.base;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

/** Snapshot-backed participant with nested rollback and final-commit notification. */
public abstract class SnapshotParticipant<T>
        implements TransactionContext.CloseCallback, TransactionContext.OuterCloseCallback {
    private final List<T> snapshots = new ArrayList<>();

    protected abstract T createSnapshot();

    protected abstract void readSnapshot(T snapshot);

    protected void releaseSnapshot(T snapshot) {
    }

    protected void onFinalCommit() {
    }

    public void updateSnapshots(TransactionContext transaction) {
        int depth = transaction.nestingDepth();
        while (snapshots.size() <= depth) snapshots.add(null);
        if (snapshots.get(depth) == null) {
            snapshots.set(depth, Objects.requireNonNull(createSnapshot(),
                    "Snapshot may not be null!"));
            transaction.addCloseCallback(this);
        }
    }

    @Override
    public void onClose(TransactionContext transaction, TransactionContext.Result result) {
        int depth = transaction.nestingDepth();
        T snapshot = snapshots.set(depth, null);
        if (result.wasAborted()) {
            readSnapshot(snapshot);
            releaseSnapshot(snapshot);
        } else if (depth > 0) {
            if (snapshots.get(depth - 1) == null) {
                snapshots.set(depth - 1, snapshot);
                transaction.getOpenTransaction(depth - 1).addCloseCallback(this);
            } else {
                releaseSnapshot(snapshot);
            }
        } else {
            releaseSnapshot(snapshot);
            transaction.addOuterCloseCallback(this);
        }
    }

    @Override
    public void afterOuterClose(TransactionContext.Result result) {
        onFinalCommit();
    }
}
