package dev.loaderbridge.fabric.remap;

import java.io.IOException;

public final class ArtifactVerificationException extends IOException {
    private static final long serialVersionUID = 1L;

    public ArtifactVerificationException(String message) {
        super(message);
    }
}
