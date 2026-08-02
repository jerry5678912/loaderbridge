package dev.loaderbridge.repository.curseforge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.loaderbridge.api.repository.RepositoryProvider;
import dev.loaderbridge.api.repository.RepositoryQuery;
import dev.loaderbridge.api.repository.RepositorySort;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;

class CurseForgeRepositoryProviderLiveTest {
    @Test
    void discoversProviderThroughServiceLoader() {
        assertThat(ServiceLoader.load(RepositoryProvider.class).stream()
                .map(provider -> provider.type().getName()))
                .contains(CurseForgeRepositoryProvider.class.getName());
    }

    @Test
    void readsCurrentFabric1211MetadataWhenCredentialsAreProvided() throws Exception {
        assumeTrue("true".equalsIgnoreCase(System.getenv("LOADERBRIDGE_LIVE"))
                && System.getenv("CURSEFORGE_API_KEY") != null);
        var provider = new CurseForgeRepositoryProvider();

        var page = provider.search(new RepositoryQuery("1.21.1", "fabric", 0, 1,
                RepositorySort.DOWNLOADS));

        assertThat(page.projects()).isNotEmpty();
        assertThat(provider.versions(page.projects().getFirst().projectId(), "1.21.1", "fabric"))
                .allMatch(artifact -> artifact.isEligibleFabric1211());
    }
}
