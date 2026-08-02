package dev.loaderbridge.catalog;

import dev.loaderbridge.api.repository.RepositoryArtifact;
import dev.loaderbridge.api.repository.RepositoryProject;
import java.util.Objects;

public record CatalogEntry(int rank, RepositoryProject project, RepositoryArtifact artifact) {
    public CatalogEntry {
        if (rank < 1) {
            throw new IllegalArgumentException("Catalog rank must be positive");
        }
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(artifact, "artifact");
    }
}
