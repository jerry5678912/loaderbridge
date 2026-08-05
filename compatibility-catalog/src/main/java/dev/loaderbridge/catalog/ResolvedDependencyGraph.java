package dev.loaderbridge.catalog;

import dev.loaderbridge.api.repository.RepositoryArtifact;
import java.util.List;
import java.util.Objects;

public record ResolvedDependencyGraph(List<RepositoryArtifact> installationOrder,
        List<ResolvedDependencyEdge> resolvedEdges) {
    public ResolvedDependencyGraph(List<RepositoryArtifact> installationOrder) {
        this(installationOrder, List.of());
    }

    public ResolvedDependencyGraph {
        installationOrder = List.copyOf(Objects.requireNonNull(installationOrder, "installationOrder"));
        resolvedEdges = List.copyOf(Objects.requireNonNull(resolvedEdges, "resolvedEdges"));
    }
}
