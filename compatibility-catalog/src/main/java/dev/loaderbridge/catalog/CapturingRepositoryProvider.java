package dev.loaderbridge.catalog;

import dev.loaderbridge.api.repository.RepositoryArtifact;
import dev.loaderbridge.api.repository.RepositoryId;
import dev.loaderbridge.api.repository.RepositoryPage;
import dev.loaderbridge.api.repository.RepositoryProvider;
import dev.loaderbridge.api.repository.RepositoryQuery;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Records successful normalized repository reads while delegating live requests. */
public final class CapturingRepositoryProvider implements RepositoryProvider {
    private final RepositoryProvider delegate;
    private final ConcurrentMap<RepositoryQuery, RepositoryPage> searches = new ConcurrentHashMap<>();
    private final ConcurrentMap<VersionsKey, List<RepositoryArtifact>> versions =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<String, PinnedResult> pinnedVersions = new ConcurrentHashMap<>();

    public CapturingRepositoryProvider(RepositoryProvider delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public RepositoryId id() {
        return delegate.id();
    }

    @Override
    public RepositoryPage search(RepositoryQuery query) throws IOException {
        RepositoryPage result = delegate.search(query);
        recordConsistently(searches, query, result);
        return result;
    }

    @Override
    public List<RepositoryArtifact> versions(String projectId, String minecraftVersion,
            String loader) throws IOException {
        VersionsKey key = new VersionsKey(projectId, minecraftVersion, loader);
        List<RepositoryArtifact> result = List.copyOf(
                delegate.versions(projectId, minecraftVersion, loader));
        recordConsistently(versions, key, result);
        return result;
    }

    @Override
    public Optional<RepositoryArtifact> versionById(String versionId) throws IOException {
        PinnedResult result = new PinnedResult(delegate.versionById(versionId));
        recordConsistently(pinnedVersions, versionId, result);
        return result.artifact();
    }

    @Override
    public Path download(RepositoryArtifact artifact, Path cacheDirectory) throws IOException {
        return delegate.download(artifact, cacheDirectory);
    }

    List<CatalogInputCapture.CapturedSearch> capturedSearches() {
        return searches.entrySet().stream().sorted(Comparator
                .comparingInt((java.util.Map.Entry<RepositoryQuery, RepositoryPage> entry) ->
                        entry.getKey().offset())
                .thenComparing(entry -> entry.getKey().minecraftVersion())
                .thenComparing(entry -> entry.getKey().loader())
                .thenComparingInt(entry -> entry.getKey().limit())
                .thenComparing(entry -> entry.getKey().sort().name()))
                .map(entry -> new CatalogInputCapture.CapturedSearch(id(), entry.getKey(),
                        entry.getValue())).toList();
    }

    List<CatalogInputCapture.CapturedVersions> capturedVersions() {
        return versions.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
                .map(entry -> new CatalogInputCapture.CapturedVersions(id(), entry.getKey().projectId(),
                        entry.getKey().minecraftVersion(), entry.getKey().loader(), entry.getValue()))
                .toList();
    }

    List<CatalogInputCapture.CapturedPinnedVersion> capturedPinnedVersions() {
        return pinnedVersions.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
                .map(entry -> new CatalogInputCapture.CapturedPinnedVersion(id(), entry.getKey(),
                        entry.getValue().artifact())).toList();
    }

    public static CatalogInputCapture capture(List<CapturingRepositoryProvider> providers,
            int targetSize, int repositoryQuota, String snapshotId, java.time.Instant frozenAt) {
        List<CapturingRepositoryProvider> ordered = new ArrayList<>(List.copyOf(providers));
        ordered.sort(Comparator.comparing(provider -> provider.id().value()));
        return new CatalogInputCapture(1, snapshotId, frozenAt, targetSize, repositoryQuota,
                ordered.stream().flatMap(provider -> provider.capturedSearches().stream()).toList(),
                ordered.stream().flatMap(provider -> provider.capturedVersions().stream()).toList(),
                ordered.stream().flatMap(provider -> provider.capturedPinnedVersions().stream()).toList());
    }

    private static <K, V> void recordConsistently(ConcurrentMap<K, V> destination, K key, V value)
            throws IOException {
        V previous = destination.putIfAbsent(key, value);
        if (previous != null && !previous.equals(value)) {
            throw new IOException("Repository returned inconsistent metadata for " + key);
        }
    }

    private record VersionsKey(String projectId, String minecraftVersion, String loader)
            implements Comparable<VersionsKey> {
        private VersionsKey {
            Objects.requireNonNull(projectId, "projectId");
            Objects.requireNonNull(minecraftVersion, "minecraftVersion");
            Objects.requireNonNull(loader, "loader");
        }

        @Override
        public int compareTo(VersionsKey other) {
            int project = projectId.compareTo(other.projectId);
            int minecraft = minecraftVersion.compareTo(other.minecraftVersion);
            return project != 0 ? project : minecraft != 0 ? minecraft : loader.compareTo(other.loader);
        }
    }

    private record PinnedResult(Optional<RepositoryArtifact> artifact) {
        private PinnedResult {
            Objects.requireNonNull(artifact, "artifact");
        }
    }
}
