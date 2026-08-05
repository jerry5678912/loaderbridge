package dev.loaderbridge.catalog;

import dev.loaderbridge.api.repository.RepositoryArtifact;
import dev.loaderbridge.api.repository.RepositoryId;
import dev.loaderbridge.api.repository.RepositoryPage;
import dev.loaderbridge.api.repository.RepositoryProvider;
import dev.loaderbridge.api.repository.RepositoryQuery;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Offline repository provider backed only by a captured catalog input bundle. */
public final class ReplayRepositoryProvider implements RepositoryProvider {
    private final RepositoryId id;
    private final Map<RepositoryQuery, RepositoryPage> searches = new HashMap<>();
    private final Map<VersionsKey, List<RepositoryArtifact>> versions = new HashMap<>();
    private final Map<String, Optional<RepositoryArtifact>> pinnedVersions = new HashMap<>();

    private ReplayRepositoryProvider(RepositoryId id) {
        this.id = id;
    }

    public static List<RepositoryProvider> from(CatalogInputCapture capture) {
        Map<RepositoryId, ReplayRepositoryProvider> providers = new java.util.TreeMap<>(
                java.util.Comparator.comparing(RepositoryId::value));
        capture.searches().forEach(item -> providers.computeIfAbsent(item.repository(),
                ReplayRepositoryProvider::new).putSearch(item));
        capture.versions().forEach(item -> providers.computeIfAbsent(item.repository(),
                ReplayRepositoryProvider::new).putVersions(item));
        capture.pinnedVersions().forEach(item -> providers.computeIfAbsent(item.repository(),
                ReplayRepositoryProvider::new).putPinned(item));
        if (providers.isEmpty()) {
            throw new IllegalArgumentException("Catalog input capture has no providers");
        }
        return List.copyOf(providers.values());
    }

    @Override
    public RepositoryId id() {
        return id;
    }

    @Override
    public RepositoryPage search(RepositoryQuery query) throws IOException {
        return required(searches.get(query), "search", query.toString());
    }

    @Override
    public List<RepositoryArtifact> versions(String projectId, String minecraftVersion,
            String loader) throws IOException {
        return required(versions.get(new VersionsKey(projectId, minecraftVersion, loader)),
                "versions", projectId + "/" + minecraftVersion + "/" + loader);
    }

    @Override
    public Optional<RepositoryArtifact> versionById(String versionId) throws IOException {
        return required(pinnedVersions.get(versionId), "pinned version", versionId);
    }

    @Override
    public Path download(RepositoryArtifact artifact, Path cacheDirectory) throws IOException {
        throw new IOException("Captured repository inputs cannot download artifacts");
    }

    private void putSearch(CatalogInputCapture.CapturedSearch item) {
        putUnique(searches, item.query(), item.result());
    }

    private void putVersions(CatalogInputCapture.CapturedVersions item) {
        putUnique(versions, new VersionsKey(item.projectId(), item.minecraftVersion(), item.loader()),
                item.result());
    }

    private void putPinned(CatalogInputCapture.CapturedPinnedVersion item) {
        putUnique(pinnedVersions, item.versionId(), item.result());
    }

    private static <K, V> void putUnique(Map<K, V> destination, K key, V value) {
        if (destination.putIfAbsent(key, value) != null) {
            throw new IllegalArgumentException("Duplicate captured repository request: " + key);
        }
    }

    private <T> T required(T value, String operation, String key) throws IOException {
        if (value == null) {
            throw new IOException("Capture has no " + id.value() + " " + operation + " for " + key);
        }
        return value;
    }

    private record VersionsKey(String projectId, String minecraftVersion, String loader) { }
}
