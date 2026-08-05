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
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class CatalogCollector {
    private static final int PAGE_SIZE = 50;
    private static final int RESOLUTION_WORKERS = 4;
    private static final int MAXIMUM_SEARCH_OFFSET = 10_000;
    private final List<RepositoryProvider> providers;
    private final RepositoryDependencyResolver dependencyResolver;

    public CatalogCollector(List<RepositoryProvider> providers) {
        this.providers = List.copyOf(providers).stream()
                .sorted(Comparator.comparing(provider -> provider.id().value())).toList();
        if (this.providers.isEmpty()) {
            throw new IllegalArgumentException("At least one repository provider is required");
        }
        this.dependencyResolver = new RepositoryDependencyResolver(this.providers);
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
                RepositoryPage page = RepositoryRequestRetrier.retry(provider,
                        "search at offset " + offset,
                        () -> provider.search(new RepositoryQuery("1.21.1", "fabric", offset,
                                PAGE_SIZE, RepositorySort.DOWNLOADS)));
                totals.put(id, page.total());
                offsets.put(id, Math.min(MAXIMUM_SEARCH_OFFSET, offset + PAGE_SIZE));
                candidates.addAll(resolvePage(provider, page.projects(), dependencyResolver));
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

    private static List<CatalogCandidate> resolvePage(RepositoryProvider provider,
            List<RepositoryProject> projects, RepositoryDependencyResolver dependencyResolver)
            throws IOException {
        try (var executor = Executors.newFixedThreadPool(RESOLUTION_WORKERS)) {
            List<Future<Optional<CatalogCandidate>>> futures = projects.stream()
                    .map(project -> executor.submit(() -> resolveProject(
                            provider, project, dependencyResolver))).toList();
            List<CatalogCandidate> resolved = new ArrayList<>();
            for (Future<Optional<CatalogCandidate>> future : futures) {
                try {
                    future.get().ifPresent(resolved::add);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while resolving repository catalog page", exception);
                } catch (ExecutionException exception) {
                    if (exception.getCause() instanceof IOException ioException) {
                        throw ioException;
                    }
                    throw new IOException("Unexpected repository catalog resolution failure",
                            exception.getCause());
                }
            }
            return resolved;
        }
    }

    private static Optional<CatalogCandidate> resolveProject(RepositoryProvider provider,
            RepositoryProject project, RepositoryDependencyResolver dependencyResolver)
            throws IOException {
        var fabricArtifact = RepositoryRequestRetrier.retry(provider,
                "Fabric versions for " + project.projectId(),
                () -> provider.versions(project.projectId(), "1.21.1", "fabric")).stream()
                .filter(RepositoryArtifact::isEligibleFabric1211)
                .max(Comparator.comparing(RepositoryArtifact::publishedAt)
                        .thenComparing(RepositoryArtifact::versionId));
        if (fabricArtifact.isEmpty()) {
            return Optional.empty();
        }
        boolean hasNativeForgeRelease = RepositoryRequestRetrier.retry(provider,
                "Forge versions for " + project.projectId(),
                () -> provider.versions(project.projectId(), "1.21.1", "forge")).stream()
                .anyMatch(artifact -> artifact.isEligibleFor("1.21.1", "forge"));
        if (hasNativeForgeRelease) {
            return Optional.empty();
        }
        RepositoryArtifact root = fabricArtifact.orElseThrow();
        try {
            dependencyResolver.resolveRequired(List.of(root));
            return Optional.of(new CatalogCandidate(project, root));
        } catch (UnresolvableRepositoryDependencyException exception) {
            return Optional.empty();
        }
    }

}
