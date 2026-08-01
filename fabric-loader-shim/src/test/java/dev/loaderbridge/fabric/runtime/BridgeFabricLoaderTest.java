package dev.loaderbridge.fabric.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.junit.jupiter.api.Test;

class BridgeFabricLoaderTest {
    @Test
    void exposesRegisteredModsAliasesEntrypointsAndDirectories() {
        BridgeFabricLoader loader = BridgeFabricLoader.getInstance();
        loader.configure(EnvType.SERVER, Path.of("build/test-game"));
        loader.registerMod(BridgeModContainer.create("fixture", "1.0.0", "Fixture", List.of("alias"),
                Path.of("build/fixture")));
        Runnable entrypoint = () -> {};
        loader.registerEntrypoint("main", entrypoint);

        assertThat(FabricLoader.getInstance()).isSameAs(loader);
        assertThat(loader.isModLoaded("alias")).isTrue();
        assertThat(loader.getModContainer("fixture")).isPresent();
        assertThat(loader.getEntrypoints("main", Runnable.class)).containsExactly(entrypoint);
        assertThat(loader.getEnvironmentType()).isEqualTo(EnvType.SERVER);
        assertThat(loader.getConfigDir().getFileName()).isEqualTo(Path.of("config"));
    }
}
