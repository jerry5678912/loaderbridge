package dev.loaderbridge.api;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ModInspection(
        Path artifact,
        LoaderId loader,
        String modId,
        String version,
        String environment,
        Map<String, List<String>> entrypoints,
        List<Diagnostic> diagnostics) {
    public ModInspection {
        Objects.requireNonNull(artifact, "artifact");
        Objects.requireNonNull(loader, "loader");
        Objects.requireNonNull(modId, "modId");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(environment, "environment");
        entrypoints = Map.copyOf(entrypoints);
        diagnostics = List.copyOf(diagnostics);
    }
}
