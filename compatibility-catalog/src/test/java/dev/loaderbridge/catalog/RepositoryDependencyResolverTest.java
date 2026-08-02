package dev.loaderbridge.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.loaderbridge.api.repository.DependencyKind;
import dev.loaderbridge.api.repository.HashAlgorithm;
import dev.loaderbridge.api.repository.ReleaseChannel;
import dev.loaderbridge.api.repository.RepositoryArtifact;
import dev.loaderbridge.api.repository.RepositoryDependency;
import dev.loaderbridge.api.repository.RepositoryId;
import dev.loaderbridge.api.repository.RepositoryPage;
import dev.loaderbridge.api.repository.RepositoryProvider;
import dev.loaderbridge.api.repository.RepositoryQuery;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RepositoryDependencyResolverTest {
    @Test
    void resolvesPinnedAndProjectDependenciesInInstallationOrderWithoutDuplicates() throws Exception {
        FakeProvider provider = new FakeProvider();
        RepositoryArtifact shared = artifact("shared", "shared-v1", List.of());
        RepositoryArtifact pinned = artifact("pinned", "pinned-v1", List.of(
                new RepositoryDependency("shared", null, DependencyKind.REQUIRED)));
        RepositoryArtifact root = artifact("root", "root-v1", List.of(
                new RepositoryDependency(null, "pinned-v1", DependencyKind.REQUIRED),
                new RepositoryDependency("shared", null, DependencyKind.REQUIRED),
                new RepositoryDependency("optional", null, DependencyKind.OPTIONAL)));
        provider.add(shared);
        provider.add(pinned);

        ResolvedDependencyGraph graph = new RepositoryDependencyResolver(List.of(provider))
                .resolveRequired(List.of(root));

        assertThat(graph.installationOrder()).extracting(RepositoryArtifact::versionId)
                .containsExactly("shared-v1", "pinned-v1", "root-v1");
    }

    @Test
    void reportsRequiredDependencyCycles() {
        FakeProvider provider = new FakeProvider();
        RepositoryArtifact left = artifact("left", "left-v1", List.of(
                new RepositoryDependency("right", null, DependencyKind.REQUIRED)));
        RepositoryArtifact right = artifact("right", "right-v1", List.of(
                new RepositoryDependency("left", null, DependencyKind.REQUIRED)));
        provider.add(left);
        provider.add(right);

        assertThatThrownBy(() -> new RepositoryDependencyResolver(List.of(provider))
                .resolveRequired(List.of(left))).isInstanceOf(IOException.class)
                .hasMessageContaining("cycle");
    }

    private static RepositoryArtifact artifact(String project, String version,
            List<RepositoryDependency> dependencies) {
        return new RepositoryArtifact(new RepositoryId("test"), project, version, "1.0",
                project + ".jar", URI.create("https://example.invalid/" + project + ".jar"), 10,
                Map.of(HashAlgorithm.SHA1, String.format("%040x", version.hashCode() & 0xffffffffL)),
                Instant.parse("2026-08-01T00:00:00Z"), ReleaseChannel.RELEASE, Set.of("1.21.1"),
                Set.of("fabric"), dependencies);
    }

    private static final class FakeProvider implements RepositoryProvider {
        private final RepositoryId id = new RepositoryId("test");
        private final Map<String, RepositoryArtifact> byProject = new HashMap<>();
        private final Map<String, RepositoryArtifact> byVersion = new HashMap<>();

        void add(RepositoryArtifact artifact) {
            byProject.put(artifact.projectId(), artifact);
            byVersion.put(artifact.versionId(), artifact);
        }

        @Override
        public RepositoryId id() {
            return id;
        }

        @Override
        public RepositoryPage search(RepositoryQuery query) {
            return new RepositoryPage(List.of(), 0, 0);
        }

        @Override
        public List<RepositoryArtifact> versions(String projectId, String minecraftVersion,
                String loader) {
            return Optional.ofNullable(byProject.get(projectId)).stream().toList();
        }

        @Override
        public Optional<RepositoryArtifact> versionById(String versionId) {
            return Optional.ofNullable(byVersion.get(versionId));
        }

        @Override
        public Path download(RepositoryArtifact artifact, Path cacheDirectory) {
            return cacheDirectory.resolve(artifact.fileName());
        }
    }
}
