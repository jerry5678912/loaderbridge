package dev.loaderbridge.catalog;

import dev.loaderbridge.api.repository.RepositoryProvider;
import dev.loaderbridge.api.repository.RetryableRepositoryException;
import java.io.IOException;

final class RepositoryRequestRetrier {
    static <T> T retry(RepositoryProvider provider, String operation, IoSupplier<T> request)
            throws IOException {
        RetryableRepositoryException lastFailure = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return request.get();
            } catch (RetryableRepositoryException exception) {
                lastFailure = exception;
            }
        }
        throw new IOException(provider.id().value() + " " + operation
                + " failed after 3 transport attempts", lastFailure);
    }

    @FunctionalInterface
    interface IoSupplier<T> {
        T get() throws IOException;
    }

    private RepositoryRequestRetrier() { }
}
