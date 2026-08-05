package dev.loaderbridge.api.repository;

import java.io.IOException;

/** A transient repository transport failure for an idempotent operation. */
public final class RetryableRepositoryException extends IOException {
    private static final long serialVersionUID = 1L;

    public RetryableRepositoryException(String message, IOException cause) {
        super(message, cause);
    }
}
