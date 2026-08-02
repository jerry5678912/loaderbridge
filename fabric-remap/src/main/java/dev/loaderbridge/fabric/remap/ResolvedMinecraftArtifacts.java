package dev.loaderbridge.fabric.remap;

import java.util.Objects;

public record ResolvedMinecraftArtifacts(
        String version, ResolvedArtifact clientJar, ResolvedArtifact clientMappings) {
    public ResolvedMinecraftArtifacts {
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(clientJar, "clientJar");
        Objects.requireNonNull(clientMappings, "clientMappings");
    }
}
