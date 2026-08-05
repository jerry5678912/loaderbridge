package dev.loaderbridge.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.loaderbridge.api.repository.HashAlgorithm;
import dev.loaderbridge.api.repository.ReleaseChannel;
import dev.loaderbridge.api.repository.RepositoryArtifact;
import dev.loaderbridge.api.repository.RepositoryDependency;
import dev.loaderbridge.api.repository.DependencyKind;
import dev.loaderbridge.api.repository.RepositoryId;
import dev.loaderbridge.api.repository.RepositoryPage;
import dev.loaderbridge.api.repository.RepositoryProject;
import dev.loaderbridge.api.repository.RepositoryProvider;
import dev.loaderbridge.api.repository.RepositoryQuery;
import dev.loaderbridge.api.repository.RetryableRepositoryException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CatalogCollectorTest {
    @Test
    void paginatesProvidersAndSelectsTheirLatestEligibleArtifacts() throws Exception {
        FakeProvider modrinth = new FakeProvider("modrinth", 55);
        FakeProvider curseforge = new FakeProvider("curseforge", 55);

        CatalogSnapshot snapshot = new CatalogCollector(List.of(curseforge, modrinth))
                .collectAndFreeze(102, 51, "2026-08", Instant.parse("2026-08-01T00:00:00Z"));

        assertThat(snapshot.entries()).hasSize(102);
        assertThat(snapshot.entries()).filteredOn(entry ->
                entry.project().repository().value().equals("modrinth")).hasSize(51);
        assertThat(modrinth.offsets).containsExactly(0, 50);
        assertThat(snapshot.entries()).allMatch(entry -> entry.artifact().versionNumber().equals("new"));
    }

    @Test
    void excludesProjectsThatPublishANativeForgeReleaseForTheSameMinecraftVersion() throws Exception {
        FakeProvider modrinth = new FakeProvider("modrinth", 3, true);
        FakeProvider curseforge = new FakeProvider("curseforge", 3, true);

        CatalogSnapshot snapshot = new CatalogCollector(List.of(modrinth, curseforge))
                .collectAndFreeze(4, 2, "2026-08", Instant.parse("2026-08-01T00:00:00Z"));

        assertThat(snapshot.entries()).noneMatch(entry -> entry.project().projectId().endsWith("0"));
        assertThat(modrinth.requestedLoaders).contains("fabric", "forge");
        assertThat(curseforge.requestedLoaders).contains("fabric", "forge");
    }

    @Test
    void retriesRepositoryTimeoutsThreeTimesWithoutRetryingSuccessfulMetadata() throws Exception {
        FakeProvider provider = new FakeProvider("modrinth", 1);
        provider.timeoutsRemaining = 2;

        CatalogSnapshot snapshot = new CatalogCollector(List.of(provider))
                .collectAndFreeze(1, 1, "2026-08", Instant.parse("2026-08-01T00:00:00Z"));

        assertThat(snapshot.entries()).hasSize(1);
        assertThat(provider.searchAttempts).isEqualTo(3);
        assertThat(provider.requestedLoaders).containsExactly("fabric", "forge");
    }

    @Test
    void reportsThreeExhaustedTransportAttemptsWithRepositoryContext() {
        FakeProvider provider = new FakeProvider("modrinth", 1);
        provider.timeoutsRemaining = 3;

        assertThatThrownBy(() -> new CatalogCollector(List.of(provider))
                .collectAndFreeze(1, 1, "2026-08", Instant.parse("2026-08-01T00:00:00Z")))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("modrinth", "search at offset 0", "3 transport attempts");
        assertThat(provider.searchAttempts).isEqualTo(3);
    }

    @Test
    void excludesUninstallableRequiredGraphsAndTopsUpFromLaterRanks() throws Exception {
        FakeProvider modrinth = new FakeProvider("modrinth", 4, false, true);
        FakeProvider curseforge = new FakeProvider("curseforge", 4);

        CatalogSnapshot snapshot = new CatalogCollector(List.of(modrinth, curseforge))
                .collectAndFreeze(6, 3, "2026-08", Instant.parse("2026-08-01T00:00:00Z"));

        assertThat(snapshot.entries()).hasSize(6);
        assertThat(snapshot.entries()).noneMatch(entry ->
                entry.project().repository().equals(modrinth.id())
                        && entry.project().projectId().endsWith("0"));
    }

    private static final class FakeProvider implements RepositoryProvider {
        private final RepositoryId id;
        private final int count;
        private final boolean exposesNativeForge;
        private final boolean unresolvableFirst;
        private final List<Integer> offsets = new ArrayList<>();
        private final List<String> requestedLoaders = Collections.synchronizedList(new ArrayList<>());
        private int timeoutsRemaining;
        private int searchAttempts;

        private FakeProvider(String id, int count) {
            this(id, count, false, false);
        }

        private FakeProvider(String id, int count, boolean exposesNativeForge) {
            this(id, count, exposesNativeForge, false);
        }

        private FakeProvider(String id, int count, boolean exposesNativeForge,
                boolean unresolvableFirst) {
            this.id = new RepositoryId(id);
            this.count = count;
            this.exposesNativeForge = exposesNativeForge;
            this.unresolvableFirst = unresolvableFirst;
        }

        @Override
        public RepositoryId id() {
            return id;
        }

        @Override
        public RepositoryPage search(RepositoryQuery query) throws IOException {
            searchAttempts++;
            if (timeoutsRemaining-- > 0) {
                throw new RetryableRepositoryException("fixture timeout",
                        new IOException("connection reset"));
            }
            offsets.add(query.offset());
            List<RepositoryProject> projects = new ArrayList<>();
            for (int index = query.offset(); index < Math.min(count, query.offset() + query.limit()); index++) {
                projects.add(new RepositoryProject(id, id.value() + index, "mod-" + index,
                        "Mod " + index, count - index, Optional.empty()));
            }
            return new RepositoryPage(projects, query.offset(), count);
        }

        @Override
        public List<RepositoryArtifact> versions(String projectId, String minecraftVersion, String loader) {
            requestedLoaders.add(loader);
            if (projectId.equals("missing")) {
                return List.of();
            }
            if (loader.equals("forge")) {
                return exposesNativeForge && projectId.endsWith("0")
                        ? List.of(artifact(projectId, "native-forge", 3, "forge")) : List.of();
            }
            return List.of(artifact(projectId, "old", 1), artifact(projectId, "new", 2));
        }

        private RepositoryArtifact artifact(String projectId, String version, int day) {
            return artifact(projectId, version, day, "fabric");
        }

        private RepositoryArtifact artifact(String projectId, String version, int day, String loader) {
            String hash = String.format("%040x", (id.value() + projectId + version).hashCode()
                    & 0xffffffffL);
            return new RepositoryArtifact(id, projectId, projectId + "-" + version, version,
                    projectId + ".jar", URI.create("https://example.invalid/" + projectId + ".jar"),
                    10, Map.of(HashAlgorithm.SHA1, hash), Instant.parse("2026-08-01T00:00:00Z")
                            .plusSeconds(day), ReleaseChannel.RELEASE, Set.of("1.21.1"), Set.of(loader),
                    unresolvableFirst && projectId.endsWith("0") && loader.equals("fabric")
                            ? List.of(new RepositoryDependency("missing", null,
                                    DependencyKind.REQUIRED)) : List.of());
        }

        @Override
        public Path download(RepositoryArtifact artifact, Path cacheDirectory) throws IOException {
            throw new IOException("not used");
        }
    }
}
