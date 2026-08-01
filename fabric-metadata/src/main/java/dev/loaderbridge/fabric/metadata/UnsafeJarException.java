package dev.loaderbridge.fabric.metadata;

import java.io.IOException;

public final class UnsafeJarException extends IOException {
    private static final long serialVersionUID = 1L;

    public UnsafeJarException(String message) {
        super(message);
    }
}
