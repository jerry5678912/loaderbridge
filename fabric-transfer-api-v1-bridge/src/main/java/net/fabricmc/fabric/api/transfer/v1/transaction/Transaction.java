package net.fabricmc.fabric.api.transfer.v1.transaction;

import dev.loaderbridge.fabric.api.transfer.transaction.BridgeTransactionManager;

/** Thread-confined atomic operation with optional nested checkpoints. */
public interface Transaction extends AutoCloseable, TransactionContext {
    static Transaction openOuter() {
        return BridgeTransactionManager.current().openOuter();
    }

    static boolean isOpen() {
        return getLifecycle() != Lifecycle.NONE;
    }

    static Lifecycle getLifecycle() {
        return BridgeTransactionManager.current().lifecycle();
    }

    static Transaction openNested(TransactionContext maybeParent) {
        return maybeParent == null ? openOuter() : maybeParent.openNested();
    }

    @Deprecated
    static TransactionContext getCurrentUnsafe() {
        return BridgeTransactionManager.current().currentUnsafe();
    }

    void abort();

    void commit();

    @Override
    void close();

    enum Lifecycle {
        NONE,
        OPEN,
        CLOSING,
        OUTER_CLOSING
    }
}
