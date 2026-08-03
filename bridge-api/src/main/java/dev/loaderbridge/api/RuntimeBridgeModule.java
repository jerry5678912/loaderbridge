package dev.loaderbridge.api;

import java.util.Objects;
import java.util.Map;
import java.util.Set;

/** Immutable capabilities advertised by one independently versioned runtime bridge module. */
public record RuntimeBridgeModule(
        String id,
        String contractVersion,
        String implementationVersion,
        BridgeCapability capability,
        Set<String> providedClasses,
        Map<String, String> providedModVersions) {
    public RuntimeBridgeModule {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(contractVersion, "contractVersion");
        Objects.requireNonNull(implementationVersion, "implementationVersion");
        Objects.requireNonNull(capability, "capability");
        providedClasses = Set.copyOf(providedClasses);
        providedModVersions = Map.copyOf(providedModVersions);
        if (id.isBlank() || contractVersion.isBlank() || implementationVersion.isBlank()) {
            throw new IllegalArgumentException("Bridge module identifiers and versions must not be blank");
        }
        if (providedClasses.stream().anyMatch(name -> name.isBlank() || name.indexOf('/') >= 0)) {
            throw new IllegalArgumentException("Provided classes must use nonblank binary names");
        }
        if (providedModVersions.entrySet().stream()
                .anyMatch(entry -> entry.getKey().isBlank() || entry.getValue().isBlank())) {
            throw new IllegalArgumentException("Provided mod IDs and versions must not be blank");
        }
    }
}
