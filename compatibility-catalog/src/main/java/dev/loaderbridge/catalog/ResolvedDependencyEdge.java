package dev.loaderbridge.catalog;

import dev.loaderbridge.api.repository.RepositoryDependency;
import dev.loaderbridge.api.repository.RepositoryId;
import java.util.Objects;

public record ResolvedDependencyEdge(RepositoryId ownerRepository, String ownerVersionId,
        RepositoryDependency declaredDependency, RepositoryId resolvedRepository,
        String resolvedVersionId) {
    public ResolvedDependencyEdge {
        Objects.requireNonNull(ownerRepository, "ownerRepository");
        ownerVersionId = required(ownerVersionId, "ownerVersionId");
        Objects.requireNonNull(declaredDependency, "declaredDependency");
        Objects.requireNonNull(resolvedRepository, "resolvedRepository");
        resolvedVersionId = required(resolvedVersionId, "resolvedVersionId");
    }

    private static String required(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = value.strip();
        if (normalized.isEmpty() || normalized.length() > 256
                || normalized.contains("/") || normalized.contains("\\")) {
            throw new IllegalArgumentException("Invalid " + name);
        }
        return normalized;
    }
}
