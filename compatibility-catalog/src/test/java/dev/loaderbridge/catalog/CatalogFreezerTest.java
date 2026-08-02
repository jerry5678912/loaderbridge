package dev.loaderbridge.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.loaderbridge.api.repository.HashAlgorithm;
import dev.loaderbridge.api.repository.ReleaseChannel;
import dev.loaderbridge.api.repository.RepositoryArtifact;
import dev.loaderbridge.api.repository.RepositoryId;
import dev.loaderbridge.api.repository.RepositoryProject;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CatalogFreezerTest {
    private static final Instant FROZEN_AT = Instant.parse("2026-08-01T00:00:00Z");

    @Test
    void selectsLatestEligibleVersionsAndDeduplicatesAcrossRepositories() {
        List<CatalogCandidate> candidates = new ArrayList<>();
        candidates.add(candidate("modrinth", "m1", 100, "a", "https://github.com/acme/shared", 1));
        candidates.add(candidate("modrinth", "m1", 100, "b", "https://github.com/acme/shared", 2));
        candidates.add(candidate("modrinth", "m2", 90, "c", "https://github.com/acme/two", 1));
        candidates.add(candidate("modrinth", "m3", 80, "d", "https://github.com/acme/three", 1));
        candidates.add(candidate("curseforge", "c1", 120, "e", "https://github.com/acme/shared/", 1));
        candidates.add(candidate("curseforge", "c2", 110, "f", "https://github.com/acme/four", 1));
        candidates.add(candidate("curseforge", "c3", 100, "g", "https://github.com/acme/five", 1));

        CatalogSnapshot snapshot = new CatalogFreezer().freeze(candidates, 4, 2,
                "2026-08", FROZEN_AT);

        assertThat(snapshot.entries()).extracting(entry -> entry.project().projectId())
                .containsExactly("m1", "m2", "c2", "c3");
        assertThat(snapshot.entries().getFirst().artifact().versionId()).isEqualTo("m1-b");
    }

    @Test
    void producesByteIdenticalSnapshotsForShuffledInputs() {
        List<CatalogCandidate> candidates = new ArrayList<>(List.of(
                candidate("modrinth", "m1", 100, "a", null, 1),
                candidate("modrinth", "m2", 90, "b", null, 1),
                candidate("curseforge", "c1", 100, "c", null, 1),
                candidate("curseforge", "c2", 90, "d", null, 1)));
        CatalogFreezer freezer = new CatalogFreezer();
        byte[] first = new CatalogSnapshotCodec().encode(freezer.freeze(candidates, 4, 2,
                "2026-08", FROZEN_AT));
        Collections.shuffle(candidates, new java.util.Random(42));
        byte[] second = new CatalogSnapshotCodec().encode(freezer.freeze(candidates, 4, 2,
                "2026-08", FROZEN_AT));

        assertThat(second).isEqualTo(first);
        assertThat(new String(first, StandardCharsets.UTF_8)).contains("\"snapshotId\": \"2026-08\"")
                .endsWith("\n");
    }

    @Test
    void deduplicatesWhenRepositoriesExposeDifferentPreferredHashes() {
        CatalogCandidate modrinth = candidate("modrinth", "m1", 100, "a", null, 1);
        RepositoryArtifact artifact = modrinth.artifact();
        RepositoryArtifact dualHash = new RepositoryArtifact(artifact.repository(), artifact.projectId(),
                artifact.versionId(), artifact.versionNumber(), artifact.fileName(), artifact.downloadUrl(),
                artifact.size(), Map.of(HashAlgorithm.SHA1, artifact.hashes().get(HashAlgorithm.SHA1),
                        HashAlgorithm.SHA512, "1".repeat(128)), artifact.publishedAt(), artifact.releaseChannel(),
                artifact.gameVersions(), artifact.loaders(), artifact.dependencies());
        CatalogCandidate curseForge = candidate("curseforge", "c1", 100, "a", null, 1);
        CatalogCandidate replacement = candidate("curseforge", "c2", 90, "b", null, 1);

        CatalogSnapshot snapshot = new CatalogFreezer().freeze(List.of(
                new CatalogCandidate(modrinth.project(), dualHash), curseForge, replacement),
                2, 1, "2026-08", FROZEN_AT);

        assertThat(snapshot.entries()).extracting(entry -> entry.project().projectId())
                .containsExactly("m1", "c2");
    }

    @Test
    void refusesToFreezeAnUndersizedCatalog() {
        assertThatThrownBy(() -> new CatalogFreezer().freeze(List.of(
                candidate("modrinth", "m1", 1, "a", null, 1)), 2, 1, "2026-08", FROZEN_AT))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("unique eligible");
    }

    private static CatalogCandidate candidate(String repository, String projectId, long downloads,
            String hashSeed, String source, int day) {
        RepositoryId id = new RepositoryId(repository);
        RepositoryProject project = new RepositoryProject(id, projectId, projectId, projectId,
                downloads, Optional.ofNullable(source).map(URI::create));
        String hash = String.format("%040x", hashSeed.codePointAt(0));
        RepositoryArtifact artifact = new RepositoryArtifact(id, projectId, projectId + "-" + hashSeed,
                "1.0." + day, projectId + ".jar", URI.create(repository.equals("modrinth")
                        ? "https://cdn.modrinth.com/" + projectId + ".jar"
                        : "https://edge.forgecdn.net/" + projectId + ".jar"), 10,
                Map.of(HashAlgorithm.SHA1, hash), FROZEN_AT.plusSeconds(day), ReleaseChannel.RELEASE,
                Set.of("1.21.1"), Set.of("fabric"), List.of());
        return new CatalogCandidate(project, artifact);
    }
}
