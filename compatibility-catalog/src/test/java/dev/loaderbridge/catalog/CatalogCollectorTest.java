package dev.loaderbridge.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import dev.loaderbridge.api.repository.HashAlgorithm;
import dev.loaderbridge.api.repository.ReleaseChannel;
import dev.loaderbridge.api.repository.RepositoryArtifact;
import dev.loaderbridge.api.repository.RepositoryId;
import dev.loaderbridge.api.repository.RepositoryPage;
import dev.loaderbridge.api.repository.RepositoryProject;
import dev.loaderbridge.api.repository.RepositoryProvider;
import dev.loaderbridge.api.repository.RepositoryQuery;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
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

    private static final class FakeProvider implements RepositoryProvider {
        private final RepositoryId id;
        private final int count;
        private final List<Integer> offsets = new ArrayList<>();

        private FakeProvider(String id, int count) {
            this.id = new RepositoryId(id);
            this.count = count;
        }

        @Override
        public RepositoryId id() {
            return id;
        }

        @Override
        public RepositoryPage search(RepositoryQuery query) {
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
            return List.of(artifact(projectId, "old", 1), artifact(projectId, "new", 2));
        }

        private RepositoryArtifact artifact(String projectId, String version, int day) {
            String hash = String.format("%040x", (id.value() + projectId + version).hashCode()
                    & 0xffffffffL);
            return new RepositoryArtifact(id, projectId, projectId + "-" + version, version,
                    projectId + ".jar", URI.create("https://example.invalid/" + projectId + ".jar"),
                    10, Map.of(HashAlgorithm.SHA1, hash), Instant.parse("2026-08-01T00:00:00Z")
                            .plusSeconds(day), ReleaseChannel.RELEASE, Set.of("1.21.1"), Set.of("fabric"),
                    List.of());
        }

        @Override
        public Path download(RepositoryArtifact artifact, Path cacheDirectory) throws IOException {
            throw new IOException("not used");
        }
    }
}
