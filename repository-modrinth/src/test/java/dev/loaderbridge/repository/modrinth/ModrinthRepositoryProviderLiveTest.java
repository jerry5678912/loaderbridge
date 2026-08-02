package dev.loaderbridge.repository.modrinth;

import static org.assertj.core.api.Assertions.assertThat;

import dev.loaderbridge.api.repository.RepositoryProvider;
import dev.loaderbridge.api.repository.RepositoryQuery;
import dev.loaderbridge.api.repository.RepositorySort;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class ModrinthRepositoryProviderLiveTest {
    @Test
    void isDiscoveredThroughRepositoryProviderService() {
        assertThat(ServiceLoader.load(RepositoryProvider.class).stream()
                .map(ServiceLoader.Provider::get).map(provider -> provider.id().value()))
                .contains("modrinth");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "LOADERBRIDGE_LIVE", matches = "true")
    void readsCurrentFabric1211ProjectAndVersionMetadata() throws Exception {
        ModrinthRepositoryProvider provider = new ModrinthRepositoryProvider();

        var page = provider.search(new RepositoryQuery("1.21.1", "fabric", 0, 1,
                RepositorySort.DOWNLOADS));
        var versions = provider.versions(page.projects().getFirst().projectId(), "1.21.1", "fabric");

        assertThat(page.projects()).hasSize(1);
        assertThat(versions).isNotEmpty().allMatch(version -> version.preferredHash().isPresent());
    }
}
