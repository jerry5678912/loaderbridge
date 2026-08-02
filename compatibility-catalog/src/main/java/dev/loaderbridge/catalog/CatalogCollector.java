package dev.loaderbridge.catalog;

import dev.loaderbridge.api.repository.RepositoryArtifact;
import dev.loaderbridge.api.repository.RepositoryPage;
import dev.loaderbridge.api.repository.RepositoryProject;
import dev.loaderbridge.api.repository.RepositoryProvider;
import dev.loaderbridge.api.repository.RepositoryQuery;
import dev.loaderbridge.api.repository.RepositorySort;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CatalogCollector {
    private static final int PAGE_SIZE = 50;
    private static final int MAXIMUM_SEARCH_OFFSET = 10_000;
    private final List<RepositoryProvider> providers;

    public CatalogCollector(List<RepositoryProvider> providers) {
        this.providers = List.copyOf(providers).stream()
                .sorted(Comparator.comparing(provider -> provider.id().value())).toList();
        if (this.providers.isEmpty()) {
            throw new IllegalArgumentException("At least one repository provider is required");
        }
    }

    public CatalogSnapshot collectAndFreeze(int targetSize, int repositoryQuota, String snapshotId,
            Instant frozenAt) throws IOException {
        List<CatalogCandidate> candidates = new ArrayList<>();
        Map<String, Integer> offsets = new HashMap<>();
        Map<String, Integer> totals = new HashMap<>();
        CatalogFreezer freezer = new CatalogFreezer();
        while (true) {
            boolean fetched = false;
            for (RepositoryProvider provider : providers) {
                String id = provider.id().value();
                int offset = offsets.getOrDefault(id, 0);
                int total = totals.getOrDefault(id, Integer.MAX_VALUE);
                if (offset >= total || offset >= MAXIMUM_SEARCH_OFFSET) {
                    continue;
                }
                RepositoryPage page = provider.search(new RepositoryQuery("1.21.1", "fabric", offset,
                        PAGE_SIZE, RepositorySort.DOWNLOADS));
                totals.put(id, page.total());
                offsets.put(id, Math.min(MAXIMUM_SEARCH_OFFSET, offset + PAGE_SIZE));
                for (RepositoryProject project : page.projects()) {
                    provider.versions(project.projectId(), "1.21.1", "fabric").stream()
                            .filter(RepositoryArtifact::isEligibleFabric1211)
                            .max(Comparator.comparing(RepositoryArtifact::publishedAt)
                                    .thenComparing(RepositoryArtifact::versionId))
                            .ifPresent(artifact -> candidates.add(new CatalogCandidate(project, artifact)));
                }
                fetched = true;
            }
            if (candidates.size() >= targetSize) {
                try {
                    return freezer.freeze(candidates, targetSize, repositoryQuota, snapshotId, frozenAt);
                } catch (IllegalArgumentException exception) {
                    if (!fetched) {
                        throw new IOException("Could not build requested unique catalog: "
                                + exception.getMessage(), exception);
                    }
                }
            }
            if (!fetched) {
                throw new IOException("Repositories were exhausted after finding " + candidates.size()
                        + " eligible projects; " + targetSize + " are required");
            }
        }
    }
}
