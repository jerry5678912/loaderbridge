package dev.loaderbridge.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import dev.loaderbridge.api.repository.HashAlgorithm;
import dev.loaderbridge.api.repository.ReleaseChannel;
import dev.loaderbridge.api.repository.RepositoryArtifact;
import dev.loaderbridge.api.repository.RepositoryId;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RepositoryResolutionLockCodecTest {
    @Test
    void writesDeterministicDependencyFirstLock() {
        RepositoryArtifact dependency = artifact("dependency", "dep-v1");
        RepositoryArtifact root = artifact("root", "root-v1");
        var graph = new ResolvedDependencyGraph(List.of(dependency, root));
        RepositoryResolutionLockCodec codec = new RepositoryResolutionLockCodec();

        byte[] first = codec.encode(root, graph);
        byte[] second = codec.encode(root, graph);

        assertThat(second).isEqualTo(first);
        String json = new String(first, StandardCharsets.UTF_8);
        assertThat(json).contains("\"root\": \"test:root-v1\"");
        assertThat(json.indexOf("\"versionId\": \"dep-v1\""))
                .isLessThan(json.indexOf("\"versionId\": \"root-v1\""));
        assertThat(json).contains("\"resolvedEdges\": []");
    }

    private static RepositoryArtifact artifact(String project, String version) {
        return new RepositoryArtifact(new RepositoryId("test"), project, version, "1.0",
                project + ".jar", URI.create("https://example.invalid/" + project + ".jar"), 10,
                Map.of(HashAlgorithm.SHA1, String.format("%040x", version.hashCode() & 0xffffffffL)),
                Instant.parse("2026-08-01T00:00:00Z"), ReleaseChannel.RELEASE, Set.of("1.21.1"),
                Set.of("fabric"), List.of());
    }
}
