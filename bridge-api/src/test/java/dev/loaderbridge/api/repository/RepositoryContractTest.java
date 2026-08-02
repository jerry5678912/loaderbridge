package dev.loaderbridge.api.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RepositoryContractTest {
    @Test
    void acceptsAValidFabricArtifactWithVerifiedHashesAndDependencies() {
        RepositoryArtifact artifact = new RepositoryArtifact(new RepositoryId("modrinth"), "project-1",
                "version-1", "1.2.3", "example.jar", URI.create("https://cdn.modrinth.com/example.jar"),
                1234, Map.of(HashAlgorithm.SHA1, "0123456789abcdef0123456789abcdef01234567"),
                Instant.parse("2026-08-01T00:00:00Z"), ReleaseChannel.RELEASE,
                Set.of("1.21.1"), Set.of("fabric"),
                List.of(new RepositoryDependency("required-project", null, DependencyKind.REQUIRED)));

        assertThat(artifact.isEligibleFabric1211()).isTrue();
        assertThat(artifact.preferredHash()).hasValueSatisfying(hash -> {
            assertThat(hash.algorithm()).isEqualTo(HashAlgorithm.SHA1);
            assertThat(hash.value()).hasSize(40);
        });
    }

    @Test
    void rejectsUnboundedPagesAndNonHttpsDownloads() {
        assertThatThrownBy(() -> new RepositoryQuery("1.21.1", "fabric", 0, 101,
                RepositorySort.DOWNLOADS)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RepositoryArtifact(new RepositoryId("curseforge"), "1", "2", "1.0",
                "unsafe.jar", URI.create("http://example.invalid/unsafe.jar"), 10,
                Map.of(HashAlgorithm.SHA1, "0123456789abcdef0123456789abcdef01234567"),
                Instant.EPOCH, ReleaseChannel.RELEASE, Set.of("1.21.1"), Set.of("fabric"), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMalformedRepositoryIdentifiersAndHashes() {
        assertThatThrownBy(() -> new RepositoryId("../../secret"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ArtifactHash(HashAlgorithm.SHA512, "abcd"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsCredentialBearingSourceUrlsAndInconsistentPages() {
        assertThatThrownBy(() -> new RepositoryProject(new RepositoryId("modrinth"), "id", "slug", "Title",
                1, java.util.Optional.of(URI.create("https://token@example.com/source"))))
                .isInstanceOf(IllegalArgumentException.class);
        RepositoryProject project = new RepositoryProject(new RepositoryId("modrinth"), "id", "slug", "Title",
                1, java.util.Optional.empty());
        assertThatThrownBy(() -> new RepositoryPage(List.of(project), 10, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allowsVersionPinnedDependenciesWithoutAProjectId() {
        RepositoryDependency dependency = new RepositoryDependency(null, "version-only", DependencyKind.REQUIRED);

        assertThat(dependency.projectId()).isNull();
        assertThat(dependency.versionId()).isEqualTo("version-only");
        assertThatThrownBy(() -> new RepositoryDependency(null, null, DependencyKind.REQUIRED))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
