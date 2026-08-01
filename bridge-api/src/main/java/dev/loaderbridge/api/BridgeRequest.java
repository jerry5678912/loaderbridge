package dev.loaderbridge.api;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record BridgeRequest(
        String minecraftVersion,
        LoaderId hostLoader,
        String hostVersion,
        BridgeEnvironment environment,
        List<Path> inputArtifacts,
        Path outputDirectory,
        Path cacheDirectory) {
    public BridgeRequest {
        Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        Objects.requireNonNull(hostLoader, "hostLoader");
        Objects.requireNonNull(hostVersion, "hostVersion");
        Objects.requireNonNull(environment, "environment");
        inputArtifacts = List.copyOf(inputArtifacts);
        Objects.requireNonNull(outputDirectory, "outputDirectory");
        Objects.requireNonNull(cacheDirectory, "cacheDirectory");
    }
}
