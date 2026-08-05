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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class RepositoryDependencyResolver {
    private final Map<String, RepositoryProvider> providers;
    private final ConcurrentMap<String, RepositoryArtifact> resolvedDependencies =
            new ConcurrentHashMap<>();

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
        List<ResolvedDependencyEdge> resolvedEdges = new ArrayList<>();
        for (RepositoryArtifact root : List.copyOf(roots)) {
            visit(root, visiting, visited, ordered, resolvedEdges);
        }
        return new ResolvedDependencyGraph(ordered, resolvedEdges);
    }

    public Map<RepositoryArtifact, Path> downloadAll(ResolvedDependencyGraph graph, Path cacheDirectory)
            throws IOException {
        Map<RepositoryArtifact, Path> downloaded = new LinkedHashMap<>();
        for (RepositoryArtifact artifact : graph.installationOrder()) {
            RepositoryProvider provider = provider(artifact);
            downloaded.put(artifact, RepositoryRequestRetrier.retry(provider,
                    "download " + artifact.versionId(),
                    () -> provider.download(artifact, cacheDirectory)));
        }
        return Map.copyOf(downloaded);
    }

    private void visit(RepositoryArtifact artifact, Set<String> visiting, Set<String> visited,
            List<RepositoryArtifact> ordered, List<ResolvedDependencyEdge> resolvedEdges)
            throws IOException {
        String key = artifact.repository().value() + ":" + artifact.versionId();
        if (visited.contains(key)) {
            return;
        }
        if (!visiting.add(key)) {
            throw new UnresolvableRepositoryDependencyException(
                    "Required dependency cycle detected at " + key);
        }
        List<RepositoryDependency> required = artifact.dependencies().stream()
                .filter(dependency -> dependency.kind() == DependencyKind.REQUIRED)
                .distinct()
                .sorted(Comparator
                        .comparing((RepositoryDependency dependency) ->
                                dependency.projectId() == null ? "" : dependency.projectId())
                        .thenComparing(dependency ->
                                dependency.versionId() == null ? "" : dependency.versionId()))
                .toList();
        for (RepositoryDependency dependency : required) {
            RepositoryArtifact resolved = resolve(artifact, dependency);
            resolvedEdges.add(new ResolvedDependencyEdge(artifact.repository(), artifact.versionId(),
                    dependency, resolved.repository(), resolved.versionId()));
            visit(resolved, visiting, visited, ordered, resolvedEdges);
        }
        visiting.remove(key);
        visited.add(key);
        ordered.add(artifact);
    }

    private RepositoryArtifact resolve(RepositoryArtifact owner, RepositoryDependency dependency)
            throws IOException {
        RepositoryProvider provider = provider(owner);
        String resolutionKey = owner.repository().value() + ":"
                + (dependency.versionId() == null ? "project:" + dependency.projectId()
                        : "version:" + dependency.versionId());
        RepositoryArtifact cached = resolvedDependencies.get(resolutionKey);
        if (cached != null) {
            return cached;
        }
        RepositoryArtifact resolved;
        if (dependency.versionId() != null) {
            var pinned = RepositoryRequestRetrier.retry(provider,
                    "version " + dependency.versionId(),
                    () -> provider.versionById(dependency.versionId()));
            if (pinned.filter(artifact -> artifact.supportsLoader("fabric")).isPresent()) {
                RepositoryArtifact artifact = pinned.orElseThrow();
                if (dependency.projectId() != null
                        && !dependency.projectId().equals(artifact.projectId())) {
                    throw new UnresolvableRepositoryDependencyException(
                            "Pinned dependency project mismatch for " + dependency.versionId());
                }
                RepositoryArtifact previous = resolvedDependencies.putIfAbsent(resolutionKey, artifact);
                return previous == null ? artifact : previous;
            }
        }
        if (dependency.projectId() != null) {
            resolved = RepositoryRequestRetrier.retry(provider,
                    "Fabric versions for dependency " + dependency.projectId(),
                    () -> provider.versions(dependency.projectId(), "1.21.1", "fabric")).stream()
                    .filter(candidate -> candidate.isCompatibleWith("1.21.1", "fabric"))
                    .max(Comparator.comparing(RepositoryArtifact::publishedAt)
                            .thenComparing(RepositoryArtifact::versionId))
                    .orElseThrow(() -> new UnresolvableRepositoryDependencyException(
                            "Required dependency "
                            + owner.repository().value() + ":" + dependency.projectId()
                            + " is unavailable for " + owner.repository().value() + ":"
                            + owner.versionId()));
            RepositoryArtifact previous = resolvedDependencies.putIfAbsent(resolutionKey, resolved);
            return previous == null ? resolved : previous;
        }
        throw new UnresolvableRepositoryDependencyException(
                "Pinned dependency " + owner.repository().value() + ":"
                + dependency.versionId() + " is unavailable for " + owner.repository().value()
                + ":" + owner.versionId());
    }

    private RepositoryProvider provider(RepositoryArtifact artifact) throws IOException {
        RepositoryProvider provider = providers.get(artifact.repository().value());
        if (provider == null) {
            throw new IOException("No provider for dependency repository " + artifact.repository().value());
        }
        return provider;
    }
}
