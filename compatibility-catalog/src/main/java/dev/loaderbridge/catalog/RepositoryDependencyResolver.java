package dev.loaderbridge.catalog;

import dev.loaderbridge.api.repository.DependencyKind;
import dev.loaderbridge.api.repository.RepositoryArtifact;
import dev.loaderbridge.api.repository.RepositoryDependency;
import dev.loaderbridge.api.repository.RepositoryProvider;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RepositoryDependencyResolver {
    private final Map<String, RepositoryProvider> providers;

    public RepositoryDependencyResolver(List<RepositoryProvider> providers) {
        Map<String, RepositoryProvider> indexed = new HashMap<>();
        for (RepositoryProvider provider : List.copyOf(providers)) {
            if (indexed.put(provider.id().value(), provider) != null) {
                throw new IllegalArgumentException("Duplicate repository provider: " + provider.id().value());
            }
        }
        this.providers = Map.copyOf(indexed);
    }

    public ResolvedDependencyGraph resolveRequired(List<RepositoryArtifact> roots) throws IOException {
        List<RepositoryArtifact> ordered = new ArrayList<>();
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        for (RepositoryArtifact root : List.copyOf(roots)) {
            visit(root, visiting, visited, ordered);
        }
        return new ResolvedDependencyGraph(ordered);
    }

    public Map<RepositoryArtifact, Path> downloadAll(ResolvedDependencyGraph graph, Path cacheDirectory)
            throws IOException {
        Map<RepositoryArtifact, Path> downloaded = new LinkedHashMap<>();
        for (RepositoryArtifact artifact : graph.installationOrder()) {
            RepositoryProvider provider = provider(artifact);
            downloaded.put(artifact, provider.download(artifact, cacheDirectory));
        }
        return Map.copyOf(downloaded);
    }

    private void visit(RepositoryArtifact artifact, Set<String> visiting, Set<String> visited,
            List<RepositoryArtifact> ordered) throws IOException {
        String key = artifact.repository().value() + ":" + artifact.versionId();
        if (visited.contains(key)) {
            return;
        }
        if (!visiting.add(key)) {
            throw new IOException("Required dependency cycle detected at " + key);
        }
        for (RepositoryDependency dependency : artifact.dependencies()) {
            if (dependency.kind() != DependencyKind.REQUIRED) {
                continue;
            }
            visit(resolve(artifact, dependency), visiting, visited, ordered);
        }
        visiting.remove(key);
        visited.add(key);
        ordered.add(artifact);
    }

    private RepositoryArtifact resolve(RepositoryArtifact owner, RepositoryDependency dependency)
            throws IOException {
        RepositoryProvider provider = provider(owner);
        if (dependency.versionId() != null) {
            var pinned = provider.versionById(dependency.versionId());
            if (pinned.filter(RepositoryArtifact::isEligibleFabric1211).isPresent()) {
                RepositoryArtifact artifact = pinned.orElseThrow();
                if (dependency.projectId() != null
                        && !dependency.projectId().equals(artifact.projectId())) {
                    throw new IOException("Pinned dependency project mismatch for " + dependency.versionId());
                }
                return artifact;
            }
        }
        if (dependency.projectId() != null) {
            return provider.versions(dependency.projectId(), "1.21.1", "fabric").stream()
                    .filter(RepositoryArtifact::isEligibleFabric1211)
                    .filter(candidate -> dependency.versionId() == null
                            || dependency.versionId().equals(candidate.versionId()))
                    .max(Comparator.comparing(RepositoryArtifact::publishedAt)
                            .thenComparing(RepositoryArtifact::versionId))
                    .orElseThrow(() -> new IOException("Required dependency is unavailable: "
                            + dependency.projectId()));
        }
        throw new IOException("Pinned dependency is unavailable: " + dependency.versionId());
    }

    private RepositoryProvider provider(RepositoryArtifact artifact) throws IOException {
        RepositoryProvider provider = providers.get(artifact.repository().value());
        if (provider == null) {
            throw new IOException("No provider for dependency repository " + artifact.repository().value());
        }
        return provider;
    }
}
