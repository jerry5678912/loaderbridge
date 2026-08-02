package dev.loaderbridge.fabric.remap;

import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;

public record ResolvedArtifact(String id, URI url, String sha1, long size, Path path) {
    public ResolvedArtifact {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(sha1, "sha1");
        Objects.requireNonNull(path, "path");
    }
}
