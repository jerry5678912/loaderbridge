package dev.loaderbridge.repository.curseforge;

import java.io.IOException;
import java.net.URI;

/** HTTP failure carrying only safe response metadata, never request headers. */
final class CurseForgeHttpException extends IOException {
    private static final long serialVersionUID = 1L;
    private final int statusCode;
    private final URI uri;

    CurseForgeHttpException(String operation, int statusCode, URI uri) {
        super("CurseForge " + operation + " returned HTTP " + statusCode
                + " for " + uri.getPath());
        this.statusCode = statusCode;
        this.uri = uri;
    }

    int statusCode() {
        return statusCode;
    }

    URI uri() {
        return uri;
    }
}
