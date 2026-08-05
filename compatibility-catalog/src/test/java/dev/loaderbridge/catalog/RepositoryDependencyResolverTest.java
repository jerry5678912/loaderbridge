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
        assertThat(graph.resolvedEdges()).extracting(ResolvedDependencyEdge::resolvedVersionId)
                .containsExactly("pinned-v1", "shared-v1", "shared-v1");
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

    @Test
    void cachesSharedProjectDependencyResolutionAcrossCatalogRoots() throws Exception {
        FakeProvider provider = new FakeProvider();
        RepositoryArtifact shared = artifact("shared", "shared-v1", List.of());
        RepositoryArtifact left = artifact("left", "left-v1", List.of(
                new RepositoryDependency("shared", null, DependencyKind.REQUIRED)));
        RepositoryArtifact right = artifact("right", "right-v1", List.of(
                new RepositoryDependency("shared", null, DependencyKind.REQUIRED)));
        provider.add(shared);

        ResolvedDependencyGraph graph = new RepositoryDependencyResolver(List.of(provider))
                .resolveRequired(List.of(left, right));

        assertThat(graph.installationOrder()).extracting(RepositoryArtifact::versionId)
                .containsExactly("shared-v1", "left-v1", "right-v1");
        assertThat(provider.versionQueries).containsExactly("shared");
    }

    @Test
    void allowsCompatibleAlphaBuildsOnlyWhenResolvingRequiredDependencies() throws Exception {
        FakeProvider provider = new FakeProvider();
        RepositoryArtifact alphaLibrary = artifact("library", "library-alpha", List.of(),
                ReleaseChannel.ALPHA);
        RepositoryArtifact root = artifact("root", "root-v1", List.of(
                new RepositoryDependency("library", null, DependencyKind.REQUIRED)));
        provider.add(alphaLibrary);

        ResolvedDependencyGraph graph = new RepositoryDependencyResolver(List.of(provider))
                .resolveRequired(List.of(root));

        assertThat(alphaLibrary.isEligibleFabric1211()).isFalse();
        assertThat(alphaLibrary.isCompatibleWith("1.21.1", "fabric")).isTrue();
        assertThat(graph.installationOrder()).extracting(RepositoryArtifact::versionId)
                .containsExactly("library-alpha", "root-v1");
    }

    @Test
    void honorsExactFabricVersionPinsFromEligibleRoots() throws Exception {
        FakeProvider provider = new FakeProvider();
        RepositoryArtifact pinned = new RepositoryArtifact(new RepositoryId("test"), "library",
                "library-pinned", "1.0", "library.jar",
                URI.create("https://example.invalid/library.jar"), 10,
                Map.of(HashAlgorithm.SHA1, String.format("%040x", 42)),
                Instant.parse("2026-08-01T00:00:00Z"), ReleaseChannel.RELEASE, Set.of("1.21"),
                Set.of("fabric"), List.of());
        RepositoryArtifact root = artifact("root", "root-v1", List.of(
                new RepositoryDependency("library", "library-pinned", DependencyKind.REQUIRED)));
        provider.add(pinned);

        ResolvedDependencyGraph graph = new RepositoryDependencyResolver(List.of(provider))
                .resolveRequired(List.of(root));

        assertThat(pinned.isCompatibleWith("1.21.1", "fabric")).isFalse();
        assertThat(pinned.supportsLoader("fabric")).isTrue();
        assertThat(graph.installationOrder()).extracting(RepositoryArtifact::versionId)
                .containsExactly("library-pinned", "root-v1");
    }

    @Test
    void substitutesSameProjectFabricBuildForCrossLoaderVersionPin() throws Exception {
        FakeProvider provider = new FakeProvider();
        RepositoryArtifact forgePin = new RepositoryArtifact(new RepositoryId("test"), "library",
                "forge-pin", "1.0+forge", "library-forge.jar",
                URI.create("https://example.invalid/library-forge.jar"), 10,
                Map.of(HashAlgorithm.SHA1, String.format("%040x", 41)),
                Instant.parse("2026-08-01T00:00:00Z"), ReleaseChannel.RELEASE, Set.of("1.21.1"),
                Set.of("forge"), List.of());
        RepositoryArtifact fabricAlternative = artifact("library", "fabric-v1", List.of());
        RepositoryArtifact root = artifact("root", "root-v1", List.of(
                new RepositoryDependency("library", "forge-pin", DependencyKind.REQUIRED)));
        provider.add(forgePin);
        provider.add(fabricAlternative);

        ResolvedDependencyGraph graph = new RepositoryDependencyResolver(List.of(provider))
                .resolveRequired(List.of(root));

        assertThat(graph.installationOrder()).extracting(RepositoryArtifact::versionId)
                .containsExactly("fabric-v1", "root-v1");
        assertThat(graph.resolvedEdges()).singleElement().satisfies(edge -> {
            assertThat(edge.declaredDependency().versionId()).isEqualTo("forge-pin");
            assertThat(edge.resolvedVersionId()).isEqualTo("fabric-v1");
        });
    }

    @Test
    void deduplicatesIdenticalRequiredDependencyDeclarationsPerOwner() throws Exception {
        FakeProvider provider = new FakeProvider();
        RepositoryArtifact shared = artifact("shared", "shared-v1", List.of());
        RepositoryDependency duplicate = new RepositoryDependency(
                "shared", null, DependencyKind.REQUIRED);
        RepositoryArtifact root = artifact("root", "root-v1", List.of(duplicate, duplicate));
        provider.add(shared);

        ResolvedDependencyGraph graph = new RepositoryDependencyResolver(List.of(provider))
                .resolveRequired(List.of(root));

        assertThat(graph.resolvedEdges()).hasSize(1);
        assertThat(provider.versionQueries).containsExactly("shared");
    }

    private static RepositoryArtifact artifact(String project, String version,
            List<RepositoryDependency> dependencies) {
        return artifact(project, version, dependencies, ReleaseChannel.RELEASE);
    }

    private static RepositoryArtifact artifact(String project, String version,
            List<RepositoryDependency> dependencies, ReleaseChannel channel) {
        return new RepositoryArtifact(new RepositoryId("test"), project, version, "1.0",
                project + ".jar", URI.create("https://example.invalid/" + project + ".jar"), 10,
                Map.of(HashAlgorithm.SHA1, String.format("%040x", version.hashCode() & 0xffffffffL)),
                Instant.parse("2026-08-01T00:00:00Z"), channel, Set.of("1.21.1"),
                Set.of("fabric"), dependencies);
    }

    private static final class FakeProvider implements RepositoryProvider {
        private final RepositoryId id = new RepositoryId("test");
        private final Map<String, RepositoryArtifact> byProject = new HashMap<>();
        private final Map<String, RepositoryArtifact> byVersion = new HashMap<>();
        private final java.util.ArrayList<String> versionQueries = new java.util.ArrayList<>();

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
            versionQueries.add(projectId);
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
