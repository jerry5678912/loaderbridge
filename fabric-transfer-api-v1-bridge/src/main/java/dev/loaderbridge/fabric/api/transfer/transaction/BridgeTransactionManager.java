package dev.loaderbridge.fabric.api.transfer.transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

/** Independent thread-local implementation of Fabric's transaction contract. */
public final class BridgeTransactionManager {
    private static final ThreadLocal<BridgeTransactionManager> MANAGERS =
            ThreadLocal.withInitial(BridgeTransactionManager::new);

    private final Thread owner = Thread.currentThread();
    private final List<BridgeTransaction> stack = new ArrayList<>();
    private final List<TransactionContext.OuterCloseCallback> outerCallbacks = new ArrayList<>();
    private int depth = -1;

    private BridgeTransactionManager() {
    }

    public static BridgeTransactionManager current() {
        return MANAGERS.get();
    }

    public Transaction openOuter() {
        if (depth >= 0) {
            throw new IllegalStateException("An outer transaction is already active on this thread.");
        }
        return open();
    }

    public Transaction.Lifecycle lifecycle() {
        return depth < 0 ? Transaction.Lifecycle.NONE : stack.get(depth).lifecycle;
    }

    public TransactionContext currentUnsafe() {
        if (depth < 0) return null;
        BridgeTransaction transaction = stack.get(depth);
        if (transaction.lifecycle != Transaction.Lifecycle.OPEN) {
            throw new IllegalStateException("May not call getCurrentUnsafe() from a close callback.");
        }
        return transaction;
    }

    private Transaction open() {
        depth++;
        if (stack.size() == depth) stack.add(new BridgeTransaction(depth));
        BridgeTransaction transaction = stack.get(depth);
        transaction.lifecycle = Transaction.Lifecycle.OPEN;
        return transaction;
    }

    private void validateThread() {
        if (Thread.currentThread() != owner) {
            throw new IllegalStateException("Attempted to access transaction state from thread "
                    + Thread.currentThread().getName() + ", but this transaction is only valid on thread "
                    + owner.getName() + ".");
        }
    }

    private final class BridgeTransaction implements Transaction {
        private final int transactionDepth;
        private final List<CloseCallback> closeCallbacks = new ArrayList<>();
        private Lifecycle lifecycle = Lifecycle.NONE;

        private BridgeTransaction(int transactionDepth) {
            this.transactionDepth = transactionDepth;
        }

        @Override
        public Transaction openNested() {
            validateCurrentOpen();
            return open();
        }

        @Override
        public int nestingDepth() {
            validateThread();
            return transactionDepth;
        }

        @Override
        public Transaction getOpenTransaction(int requestedDepth) {
            validateThread();
            if (requestedDepth < 0 || requestedDepth > depth) {
                throw new IndexOutOfBoundsException(
                        "There is no open transaction for nesting depth " + requestedDepth);
            }
            BridgeTransaction transaction = stack.get(requestedDepth);
            transaction.validateOpen();
            return transaction;
        }

        @Override
        public void addCloseCallback(CloseCallback callback) {
            validateThread();
            validateOpen();
            closeCallbacks.add(Objects.requireNonNull(callback, "Close callback cannot be null"));
        }

        @Override
        public void addOuterCloseCallback(OuterCloseCallback callback) {
            validateThread();
            if (depth < 0) throw new IllegalStateException("There is no open transaction on this thread.");
            outerCallbacks.add(Objects.requireNonNull(callback,
                    "Outer close callback cannot be null"));
        }

        @Override
        public void abort() {
            finish(Result.ABORTED);
        }

        @Override
        public void commit() {
            finish(Result.COMMITTED);
        }

        @Override
        public void close() {
            validateThread();
            if (depth >= 0 && lifecycle == Lifecycle.OPEN) abort();
        }

        private void finish(Result result) {
            validateCurrentOpen();
            lifecycle = Lifecycle.CLOSING;
            RuntimeException failure = invokeCloseCallbacks(result);
            if (depth == 0) {
                lifecycle = Lifecycle.OUTER_CLOSING;
                failure = invokeOuterCallbacks(result, failure);
            }
            depth--;
            lifecycle = Lifecycle.NONE;
            if (failure != null) throw failure;
        }

        private RuntimeException invokeCloseCallbacks(Result result) {
            RuntimeException failure = null;
            for (int index = closeCallbacks.size() - 1; index >= 0; index--) {
                try {
                    closeCallbacks.get(index).onClose(this, result);
                } catch (Exception exception) {
                    failure = aggregate(failure,
                            "Encountered an exception while invoking a transaction close callback.",
                            exception);
                }
            }
            closeCallbacks.clear();
            return failure;
        }

        private RuntimeException invokeOuterCallbacks(Result result, RuntimeException failure) {
            for (int index = outerCallbacks.size() - 1; index >= 0; index--) {
                try {
                    outerCallbacks.get(index).afterOuterClose(result);
                } catch (Exception exception) {
                    failure = aggregate(failure,
                            "Encountered an exception while invoking a transaction outer close callback.",
                            exception);
                }
            }
            outerCallbacks.clear();
            return failure;
        }

        private void validateCurrentOpen() {
            validateThread();
            if (depth < 0 || stack.get(depth) != this) {
                throw new IllegalStateException("Transaction function was called on a transaction with depth "
                        + transactionDepth + ", but the current transaction has depth " + depth + ".");
            }
            validateOpen();
        }

        private void validateOpen() {
            if (lifecycle != Lifecycle.OPEN) {
                throw new IllegalStateException(
                        "Transaction operation cannot be applied to a closed transaction.");
            }
        }
    }

    private static RuntimeException aggregate(RuntimeException previous, String message,
            Exception exception) {
        if (previous == null) return new RuntimeException(message, exception);
        previous.addSuppressed(exception);
        return previous;
    }
}
