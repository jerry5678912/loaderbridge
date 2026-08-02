package dev.loaderbridge.api.repository;

import java.util.Objects;

public record RepositoryDependency(String projectId, String versionId, DependencyKind kind) {
    public RepositoryDependency {
        if (projectId != null) {
            projectId = requireIdentifier(projectId, "projectId");
        }
        if (versionId != null) {
            versionId = requireIdentifier(versionId, "versionId");
        }
        if (projectId == null && versionId == null) {
            throw new IllegalArgumentException("A dependency requires a project ID or version ID");
        }
        Objects.requireNonNull(kind, "kind");
    }

    private static String requireIdentifier(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = value.strip();
        if (normalized.isEmpty() || normalized.length() > 256 || normalized.contains("/")
                || normalized.contains("\\")) {
            throw new IllegalArgumentException("Invalid " + name);
        }
        return normalized;
    }
}
