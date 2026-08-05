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
import dev.loaderbridge.api.repository.RepositoryProject;
import dev.loaderbridge.api.repository.RepositoryProvider;
import dev.loaderbridge.api.repository.RepositoryQuery;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CatalogInputCaptureTest {
    private static final Instant FROZEN_AT = Instant.parse("2026-08-01T00:00:00Z");

    @TempDir
    Path temporaryDirectory;

    @Test
    void replaysEveryRankingAndDependencyInputWithoutTheLiveProvider() throws Exception {
        CapturingRepositoryProvider provider = new CapturingRepositoryProvider(new FixtureProvider());
        CatalogSnapshot live = new CatalogCollector(List.of(provider))
                .collectAndFreeze(1, 1, "2026-08", FROZEN_AT);
        ResolvedDependencyGraph liveGraph = new RepositoryDependencyResolver(List.of(provider))
                .resolveRequired(live.entries().stream().map(CatalogEntry::artifact).toList());
        CatalogInputCapture capture = CapturingRepositoryProvider.capture(List.of(provider),
                1, 1, "2026-08", FROZEN_AT);
        CatalogInputCaptureCodec codec = new CatalogInputCaptureCodec();
        Path captureFile = temporaryDirectory.resolve("inputs.json");
        codec.write(capture, captureFile);

        CatalogInputCapture decoded = codec.read(captureFile);
        List<RepositoryProvider> replay = ReplayRepositoryProvider.from(decoded);
        CatalogSnapshot reproduced = new CatalogCollector(replay).collectAndFreeze(
                decoded.targetSize(), decoded.repositoryQuota(), decoded.snapshotId(),
                decoded.frozenAt());
        ResolvedDependencyGraph reproducedGraph = new RepositoryDependencyResolver(replay)
                .resolveRequired(reproduced.entries().stream().map(CatalogEntry::artifact).toList());

        assertThat(new CatalogSnapshotCodec().encode(reproduced))
                .isEqualTo(new CatalogSnapshotCodec().encode(live));
        assertThat(new CatalogDependencyLockCodec().encode(reproduced, reproducedGraph))
                .isEqualTo(new CatalogDependencyLockCodec().encode(live, liveGraph));
        assertThat(codec.encode(decoded)).isEqualTo(codec.encode(capture));
        assertThat(decoded.searches()).hasSize(1);
        assertThat(decoded.versions()).hasSize(2);
        assertThat(decoded.pinnedVersions()).hasSize(1);
        assertThat(new String(codec.encode(decoded), StandardCharsets.UTF_8))
                .doesNotContain("api-key", "credential", "secret");
    }

    @Test
    void replayFailsClosedWhenARequiredRequestWasNotCaptured() {
        CatalogInputCapture capture = new CatalogInputCapture(1, "empty", FROZEN_AT, 1, 1,
                List.of(new CatalogInputCapture.CapturedSearch(new RepositoryId("test"),
                        new RepositoryQuery("1.21.1", "fabric", 0, 50,
                                dev.loaderbridge.api.repository.RepositorySort.DOWNLOADS),
                        new RepositoryPage(List.of(), 0, 0))), List.of(), List.of());
        RepositoryProvider replay = ReplayRepositoryProvider.from(capture).getFirst();

        assertThatThrownBy(() -> replay.versions("missing", "1.21.1", "fabric"))
                .isInstanceOf(IOException.class).hasMessageContaining("Capture has no", "versions");
    }

    @Test
    void canonicalizesRequestOrderAndRejectsDuplicateIdentities() {
        RepositoryId repository = new RepositoryId("test");
        RepositoryQuery firstQuery = new RepositoryQuery("1.21.1", "fabric", 0, 50,
                dev.loaderbridge.api.repository.RepositorySort.DOWNLOADS);
        RepositoryQuery secondQuery = new RepositoryQuery("1.21.1", "fabric", 50, 50,
                dev.loaderbridge.api.repository.RepositorySort.DOWNLOADS);
        var first = new CatalogInputCapture.CapturedSearch(repository, firstQuery,
                new RepositoryPage(List.of(), 0, 0));
        var second = new CatalogInputCapture.CapturedSearch(repository, secondQuery,
                new RepositoryPage(List.of(), 50, 50));

        CatalogInputCapture ordered = new CatalogInputCapture(1, "ordered", FROZEN_AT, 1, 1,
                List.of(second, first), List.of(), List.of());

        assertThat(ordered.searches()).containsExactly(first, second);
        assertThatThrownBy(() -> new CatalogInputCapture(1, "duplicate", FROZEN_AT, 1, 1,
                List.of(first, first), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate captured search request");
    }

    private static final class FixtureProvider implements RepositoryProvider {
        private final RepositoryId id = new RepositoryId("test");

        @Override
        public RepositoryId id() {
            return id;
        }

        @Override
        public RepositoryPage search(RepositoryQuery query) {
            RepositoryProject project = new RepositoryProject(id, "root", "root", "Root", 100,
                    Optional.of(URI.create("https://example.invalid/root")));
            return new RepositoryPage(List.of(project), query.offset(), 1);
        }

        @Override
        public List<RepositoryArtifact> versions(String projectId, String minecraftVersion,
                String loader) {
            if (loader.equals("forge")) {
                return List.of();
            }
            return projectId.equals("root") ? List.of(root()) : List.of();
        }

        @Override
        public Optional<RepositoryArtifact> versionById(String versionId) {
            return versionId.equals("dependency-v1") ? Optional.of(dependency()) : Optional.empty();
        }

        @Override
        public Path download(RepositoryArtifact artifact, Path cacheDirectory) throws IOException {
            throw new IOException("not used");
        }

        private RepositoryArtifact root() {
            return artifact("root", "root-v1", List.of(new RepositoryDependency("dependency",
                    "dependency-v1", DependencyKind.REQUIRED)));
        }

        private RepositoryArtifact dependency() {
            return artifact("dependency", "dependency-v1", List.of());
        }

        private RepositoryArtifact artifact(String projectId, String versionId,
                List<RepositoryDependency> dependencies) {
            return new RepositoryArtifact(id, projectId, versionId, "1.0", projectId + ".jar",
                    URI.create("https://example.invalid/" + projectId + ".jar"), 10,
                    Map.of(HashAlgorithm.SHA1, String.format("%040x", versionId.hashCode()
                            & 0xffffffffL)), FROZEN_AT, ReleaseChannel.RELEASE, Set.of("1.21.1"),
                    Set.of("fabric"), dependencies);
        }
    }
}
