package dev.loaderbridge.api;

import java.util.List;
import java.util.Objects;

/** One non-mod runtime artifact and the JVM arguments needed to activate it. */
public record RuntimeLaunchArtifact(
        String id,
        String version,
        String requiredModuleId,
        String relativeOutput,
        List<String> jvmArguments) {
    public RuntimeLaunchArtifact {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(requiredModuleId, "requiredModuleId");
        Objects.requireNonNull(relativeOutput, "relativeOutput");
        jvmArguments = List.copyOf(jvmArguments);
        if (id.isBlank() || version.isBlank() || requiredModuleId.isBlank()
                || relativeOutput.isBlank() || jvmArguments.isEmpty()
                || jvmArguments.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("Launch artifact fields must not be blank");
        }
    }
}
