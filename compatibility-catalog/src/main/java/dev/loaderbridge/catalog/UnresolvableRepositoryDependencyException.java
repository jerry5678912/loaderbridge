package dev.loaderbridge.catalog;

import java.io.IOException;

/** A root-specific required dependency graph that cannot be installed as declared. */
public final class UnresolvableRepositoryDependencyException extends IOException {
    private static final long serialVersionUID = 1L;

    UnresolvableRepositoryDependencyException(String message) {
        super(message);
    }
}
