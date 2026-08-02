package dev.loaderbridge.api;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record BridgeRequest(
        String minecraftVersion,
        LoaderId hostLoader,
        String hostVersion,
        BridgeEnvironment environment,
        List<Path> inputArtifacts,
        Path outputDirectory,
        Path cacheDirectory,
        Optional<String> sourceNamespaceOverride,
        boolean refresh) {
    public BridgeRequest {
        Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        Objects.requireNonNull(hostLoader, "hostLoader");
        Objects.requireNonNull(hostVersion, "hostVersion");
        Objects.requireNonNull(environment, "environment");
        inputArtifacts = List.copyOf(inputArtifacts);
        Objects.requireNonNull(outputDirectory, "outputDirectory");
        Objects.requireNonNull(cacheDirectory, "cacheDirectory");
        Objects.requireNonNull(sourceNamespaceOverride, "sourceNamespaceOverride");
    }

    public BridgeRequest(String minecraftVersion, LoaderId hostLoader, String hostVersion,
            BridgeEnvironment environment, List<Path> inputArtifacts, Path outputDirectory, Path cacheDirectory) {
        this(minecraftVersion, hostLoader, hostVersion, environment, inputArtifacts, outputDirectory,
                cacheDirectory, Optional.empty(), false);
    }

    public BridgeRequest(String minecraftVersion, LoaderId hostLoader, String hostVersion,
            BridgeEnvironment environment, List<Path> inputArtifacts, Path outputDirectory, Path cacheDirectory,
            Optional<String> sourceNamespaceOverride) {
        this(minecraftVersion, hostLoader, hostVersion, environment, inputArtifacts, outputDirectory,
                cacheDirectory, sourceNamespaceOverride, false);
    }
}
