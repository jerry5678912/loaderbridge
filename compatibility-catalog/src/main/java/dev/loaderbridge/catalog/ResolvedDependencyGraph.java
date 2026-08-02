package dev.loaderbridge.catalog;

import dev.loaderbridge.api.repository.RepositoryArtifact;
import java.util.List;
import java.util.Objects;

public record ResolvedDependencyGraph(List<RepositoryArtifact> installationOrder) {
    public ResolvedDependencyGraph {
        installationOrder = List.copyOf(Objects.requireNonNull(installationOrder, "installationOrder"));
    }
}
