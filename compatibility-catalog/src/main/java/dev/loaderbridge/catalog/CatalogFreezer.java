package dev.loaderbridge.catalog;

import dev.loaderbridge.api.repository.RepositoryId;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CatalogFreezer {
    private static final List<String> STANDARD_REPOSITORY_ORDER = List.of("modrinth", "curseforge");

    public CatalogSnapshot freeze(List<CatalogCandidate> source, int targetSize, int repositoryQuota,
            String snapshotId, Instant frozenAt) {
        if (targetSize < 1 || repositoryQuota < 1 || repositoryQuota > targetSize) {
            throw new IllegalArgumentException("Invalid catalog target or repository quota");
        }
        Map<String, CatalogCandidate> latestByProject = latestEligibleByProject(source);
        Map<RepositoryId, List<CatalogCandidate>> byRepository = new HashMap<>();
        latestByProject.values().forEach(candidate -> byRepository
                .computeIfAbsent(candidate.project().repository(), ignored -> new ArrayList<>()).add(candidate));
        Comparator<CatalogCandidate> rank = Comparator
                .comparingLong((CatalogCandidate candidate) -> candidate.project().downloads()).reversed()
                .thenComparing(candidate -> candidate.project().projectId());
        byRepository.values().forEach(values -> values.sort(rank));
        List<RepositoryId> repositories = byRepository.keySet().stream()
                .sorted(Comparator.comparingInt(CatalogFreezer::repositoryOrder)
                        .thenComparing(RepositoryId::value)).toList();

        List<CatalogCandidate> selected = new ArrayList<>();
        Set<String> artifactHashes = new HashSet<>();
        Set<String> sources = new HashSet<>();
        Map<RepositoryId, Integer> cursors = new LinkedHashMap<>();
        for (RepositoryId repository : repositories) {
            cursors.put(repository, 0);
            while (selected.stream().filter(item -> item.project().repository().equals(repository)).count()
                    < repositoryQuota && selected.size() < targetSize
                    && addNext(byRepository.get(repository), cursors, repository, selected,
                            artifactHashes, sources)) {
                // Keep consuming the platform-local ranking until its quota is filled.
            }
        }
        boolean progressed = true;
        while (selected.size() < targetSize && progressed) {
            progressed = false;
            for (RepositoryId repository : repositories) {
                if (selected.size() == targetSize) {
                    break;
                }
                progressed |= addNext(byRepository.get(repository), cursors, repository, selected,
                        artifactHashes, sources);
            }
        }
        if (selected.size() != targetSize) {
            throw new IllegalArgumentException("Only " + selected.size() + " unique eligible projects are available");
        }
        List<CatalogEntry> entries = new ArrayList<>(targetSize);
        for (int index = 0; index < selected.size(); index++) {
            CatalogCandidate candidate = selected.get(index);
            entries.add(new CatalogEntry(index + 1, candidate.project(), candidate.artifact()));
        }
        return new CatalogSnapshot(1, snapshotId, frozenAt, "1.21.1", "fabric", entries);
    }

    private static Map<String, CatalogCandidate> latestEligibleByProject(List<CatalogCandidate> source) {
        Map<String, CatalogCandidate> latest = new HashMap<>();
        for (CatalogCandidate candidate : List.copyOf(source)) {
            if (!candidate.artifact().isEligibleFabric1211()) {
                continue;
            }
            String key = candidate.project().repository().value() + ":" + candidate.project().projectId();
            latest.merge(key, candidate, (left, right) -> {
                int published = right.artifact().publishedAt().compareTo(left.artifact().publishedAt());
                if (published != 0) {
                    return published > 0 ? right : left;
                }
                return right.artifact().versionId().compareTo(left.artifact().versionId()) > 0
                        ? right : left;
            });
        }
        return latest;
    }

    private static boolean addNext(List<CatalogCandidate> candidates, Map<RepositoryId, Integer> cursors,
            RepositoryId repository, List<CatalogCandidate> selected, Set<String> hashes,
            Set<String> sources) {
        int cursor = cursors.get(repository);
        while (cursor < candidates.size()) {
            CatalogCandidate candidate = candidates.get(cursor++);
            cursors.put(repository, cursor);
            Set<String> candidateHashes = candidate.artifact().hashes().entrySet().stream()
                    .map(hash -> hash.getKey().name() + ":" + hash.getValue()).collect(
                            java.util.stream.Collectors.toUnmodifiableSet());
            String source = candidate.project().sourceUrl().map(CatalogFreezer::canonicalSource).orElse(null);
            if (candidateHashes.stream().anyMatch(hashes::contains)
                    || source != null && sources.contains(source)) {
                continue;
            }
            hashes.addAll(candidateHashes);
            if (source != null) {
                sources.add(source);
            }
            selected.add(candidate);
            return true;
        }
        return false;
    }

    private static String canonicalSource(URI source) {
        try {
            String path = source.getPath();
            while (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            return new URI(source.getScheme().toLowerCase(Locale.ROOT), null,
                    source.getHost().toLowerCase(Locale.ROOT), source.getPort(), path, null, null).toString();
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid canonical source URL", exception);
        }
    }

    private static int repositoryOrder(RepositoryId repository) {
        int index = STANDARD_REPOSITORY_ORDER.indexOf(repository.value());
        return index < 0 ? Integer.MAX_VALUE : index;
    }
}
