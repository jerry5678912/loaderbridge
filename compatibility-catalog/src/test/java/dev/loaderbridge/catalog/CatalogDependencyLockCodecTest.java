package dev.loaderbridge.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import dev.loaderbridge.api.repository.HashAlgorithm;
import dev.loaderbridge.api.repository.ReleaseChannel;
import dev.loaderbridge.api.repository.RepositoryArtifact;
import dev.loaderbridge.api.repository.RepositoryId;
import dev.loaderbridge.api.repository.RepositoryProject;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CatalogDependencyLockCodecTest {
    @TempDir
    java.nio.file.Path temporaryDirectory;

    @Test
    void locksSnapshotIdentityRootsAndDependencyFirstArtifactsDeterministically() throws Exception {
        RepositoryArtifact dependency = artifact("dependency", "dependency-v1");
        RepositoryArtifact root = artifact("root", "root-v1");
        RepositoryProject project = new RepositoryProject(new RepositoryId("test"), "root",
                "root", "Root", 10, Optional.empty());
        CatalogSnapshot snapshot = new CatalogSnapshot(1, "2026-08",
                Instant.parse("2026-08-01T00:00:00Z"), "1.21.1", "fabric",
                List.of(new CatalogEntry(1, project, root)));
        var graph = new ResolvedDependencyGraph(List.of(dependency, root));
        CatalogDependencyLockCodec codec = new CatalogDependencyLockCodec();

        byte[] first = codec.encode(snapshot, graph);
        byte[] second = codec.encode(snapshot, graph);

        assertThat(second).isEqualTo(first);
        String json = new String(first, StandardCharsets.UTF_8);
        String snapshotHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(new CatalogSnapshotCodec().encode(snapshot)));
        assertThat(json).contains("\"snapshotId\": \"2026-08\"",
                "\"snapshotSha256\": \"" + snapshotHash + "\"",
                "\"test:root-v1\"");
        assertThat(json.indexOf("\"versionId\": \"dependency-v1\""))
                .isLessThan(json.indexOf("\"versionId\": \"root-v1\""));
        assertThat(json).contains("\"resolvedEdges\": []");
    }

    @Test
    void snapshotCodecRoundTripsValidatedArtifactsForOfflineLocking() throws Exception {
        RepositoryArtifact root = artifact("root", "root-v1");
        RepositoryProject project = new RepositoryProject(new RepositoryId("test"), "root",
                "root", "Root", 10, Optional.of(URI.create("https://example.invalid/source")));
        CatalogSnapshot snapshot = new CatalogSnapshot(1, "2026-08",
                Instant.parse("2026-08-01T00:00:00Z"), "1.21.1", "fabric",
                List.of(new CatalogEntry(1, project, root)));
        CatalogSnapshotCodec codec = new CatalogSnapshotCodec();
        java.nio.file.Path path = temporaryDirectory.resolve("snapshot.json");
        codec.write(snapshot, path);

        CatalogSnapshot decoded = codec.read(path);

        assertThat(codec.encode(decoded)).isEqualTo(codec.encode(snapshot));
    }

    private static RepositoryArtifact artifact(String project, String version) {
        return new RepositoryArtifact(new RepositoryId("test"), project, version, "1.0",
                project + ".jar", URI.create("https://example.invalid/" + project + ".jar"), 10,
                Map.of(HashAlgorithm.SHA1, String.format("%040x", version.hashCode() & 0xffffffffL)),
                Instant.parse("2026-08-01T00:00:00Z"), ReleaseChannel.RELEASE, Set.of("1.21.1"),
                Set.of("fabric"), List.of());
    }
}
