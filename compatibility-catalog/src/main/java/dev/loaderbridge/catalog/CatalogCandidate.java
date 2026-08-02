package dev.loaderbridge.catalog;

import dev.loaderbridge.api.repository.RepositoryArtifact;
import dev.loaderbridge.api.repository.RepositoryProject;
import java.util.Objects;

public record CatalogCandidate(RepositoryProject project, RepositoryArtifact artifact) {
    public CatalogCandidate {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(artifact, "artifact");
        if (!project.repository().equals(artifact.repository())
                || !project.projectId().equals(artifact.projectId())) {
            throw new IllegalArgumentException("Catalog project and artifact identities must match");
        }
    }
}
