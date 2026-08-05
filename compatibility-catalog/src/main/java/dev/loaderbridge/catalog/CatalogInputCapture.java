package dev.loaderbridge.catalog;

import dev.loaderbridge.api.repository.RepositoryArtifact;
import dev.loaderbridge.api.repository.RepositoryId;
import dev.loaderbridge.api.repository.RepositoryPage;
import dev.loaderbridge.api.repository.RepositoryQuery;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable repository metadata inputs used to produce one catalog snapshot. */
public record CatalogInputCapture(int schemaVersion, String snapshotId, Instant frozenAt,
        int targetSize, int repositoryQuota, List<CapturedSearch> searches,
        List<CapturedVersions> versions, List<CapturedPinnedVersion> pinnedVersions) {
    public CatalogInputCapture {
        if (schemaVersion != 1) {
            throw new IllegalArgumentException("Unsupported catalog input schema " + schemaVersion);
        }
        snapshotId = required(snapshotId, "snapshotId");
        Objects.requireNonNull(frozenAt, "frozenAt");
        if (targetSize < 1 || repositoryQuota < 1 || repositoryQuota > targetSize) {
            throw new IllegalArgumentException("Invalid catalog target or repository quota");
        }
        searches = Objects.requireNonNull(searches, "searches").stream().sorted(Comparator
                .comparing((CapturedSearch item) -> item.repository().value())
                .thenComparingInt(item -> item.query().offset())
                .thenComparing(item -> item.query().minecraftVersion())
                .thenComparing(item -> item.query().loader())
                .thenComparingInt(item -> item.query().limit())
                .thenComparing(item -> item.query().sort().name())).toList();
        versions = Objects.requireNonNull(versions, "versions").stream().sorted(Comparator
                .comparing((CapturedVersions item) -> item.repository().value())
                .thenComparing(CapturedVersions::projectId)
                .thenComparing(CapturedVersions::minecraftVersion)
                .thenComparing(CapturedVersions::loader)).toList();
        pinnedVersions = Objects.requireNonNull(pinnedVersions, "pinnedVersions").stream()
                .sorted(Comparator
                        .comparing((CapturedPinnedVersion item) -> item.repository().value())
                        .thenComparing(CapturedPinnedVersion::versionId)).toList();
        rejectDuplicateRequests(searches.stream().map(item -> item.repository().value()
                + "\u0000" + item.query()).toList(), "search");
        rejectDuplicateRequests(versions.stream().map(item -> item.repository().value()
                + "\u0000" + item.projectId() + "\u0000" + item.minecraftVersion()
                + "\u0000" + item.loader()).toList(), "versions");
        rejectDuplicateRequests(pinnedVersions.stream().map(item -> item.repository().value()
                + "\u0000" + item.versionId()).toList(), "pinned version");
    }

    public record CapturedSearch(RepositoryId repository, RepositoryQuery query,
            RepositoryPage result) {
        public CapturedSearch {
            Objects.requireNonNull(repository, "repository");
            Objects.requireNonNull(query, "query");
            Objects.requireNonNull(result, "result");
            if (!result.projects().stream().allMatch(project -> project.repository().equals(repository))) {
                throw new IllegalArgumentException("Captured search contains another repository");
            }
        }
    }

    public record CapturedVersions(RepositoryId repository, String projectId,
            String minecraftVersion, String loader, List<RepositoryArtifact> result) {
        public CapturedVersions {
            Objects.requireNonNull(repository, "repository");
            String normalizedProjectId = required(projectId, "projectId");
            projectId = normalizedProjectId;
            minecraftVersion = required(minecraftVersion, "minecraftVersion");
            loader = required(loader, "loader");
            result = List.copyOf(Objects.requireNonNull(result, "result"));
            if (!result.stream().allMatch(artifact -> artifact.repository().equals(repository)
                    && artifact.projectId().equals(normalizedProjectId))) {
                throw new IllegalArgumentException("Captured versions do not match their request");
            }
        }
    }

    public record CapturedPinnedVersion(RepositoryId repository, String versionId,
            Optional<RepositoryArtifact> result) {
        public CapturedPinnedVersion {
            Objects.requireNonNull(repository, "repository");
            versionId = required(versionId, "versionId");
            result = Objects.requireNonNull(result, "result");
            if (result.isPresent() && (!result.orElseThrow().repository().equals(repository)
                    || !result.orElseThrow().versionId().equals(versionId))) {
                throw new IllegalArgumentException("Captured pinned version does not match its request");
            }
        }
    }

    private static String required(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = value.strip();
        if (normalized.isEmpty() || normalized.length() > 256) {
            throw new IllegalArgumentException("Invalid " + name);
        }
        return normalized;
    }

    private static void rejectDuplicateRequests(List<String> keys, String kind) {
        if (new HashSet<>(keys).size() != keys.size()) {
            throw new IllegalArgumentException("Duplicate captured " + kind + " request");
        }
    }
}
