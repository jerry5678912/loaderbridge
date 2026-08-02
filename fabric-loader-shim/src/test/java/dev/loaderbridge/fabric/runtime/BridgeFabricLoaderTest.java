package dev.loaderbridge.fabric.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
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

    @Test
    void exposesEntrypointContainersAndAggregatesInvocationFailures() {
        BridgeFabricLoader loader = BridgeFabricLoader.getInstance();
        loader.resetForTests();
        ModContainer first = BridgeModContainer.create("first", "1", "First", List.of(), Path.of("first"));
        ModContainer second = BridgeModContainer.create("second", "1", "Second", List.of(), Path.of("second"));
        Runnable one = () -> { throw new IllegalStateException("one"); };
        Runnable two = () -> { throw new IllegalArgumentException("two"); };
        loader.registerEntrypoint("main", first, "example.One", one);
        loader.registerEntrypoint("main", second, "example.Two", two);

        assertThat(loader.getEntrypointContainers("main", Runnable.class))
                .extracting(container -> container.getProvider().getMetadata().getId())
                .containsExactly("first", "second");
        assertThat(loader.getEntrypointContainers("main", Runnable.class))
                .extracting(container -> container.getDefinition())
                .containsExactly("example.One", "example.Two");
        assertThatThrownBy(() -> loader.invokeEntrypoints("main", Runnable.class, Runnable::run))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("provided by 'first'")
                .satisfies(error -> assertThat(error.getSuppressed()).hasSize(1));
    }

    @Test
    void objectShareValidatesKeysAndNotifiesDeferredAndImmediateConsumers() {
        BridgeFabricLoader loader = BridgeFabricLoader.getInstance();
        loader.resetForTests();
        var share = loader.getObjectShare();
        List<String> notifications = new ArrayList<>();

        share.whenAvailable("fixture:value", (key, value) -> notifications.add(key + "=" + value));
        assertThat(share.put("fixture:value", 42)).isNull();
        share.whenAvailable("fixture:value", (key, value) -> notifications.add("again=" + value));

        assertThat(notifications).containsExactly("fixture:value=42", "again=42");
        assertThatIllegalArgumentException().isThrownBy(() -> share.get("invalid"));
        assertThatNullPointerException().isThrownBy(() -> share.put("fixture:null", null));
    }

    @Test
    @SuppressWarnings("deprecation")
    void exposesRuntimeContextAndSanitizesLaunchArguments() {
        BridgeFabricLoader loader = BridgeFabricLoader.getInstance();
        loader.resetForTests();
        Object game = new Object();
        loader.configure(EnvType.CLIENT, Path.of("build/test-game"), true, game,
                new String[] {"--username", "Jerry", "--demo", "--accessToken", "secret"});

        assertThat(loader.isDevelopmentEnvironment()).isTrue();
        assertThat(loader.getGameInstance()).isSameAs(game);
        assertThat(loader.getGameDirectory()).isEqualTo(loader.getGameDir().toFile());
        assertThat(loader.getConfigDirectory()).isEqualTo(loader.getConfigDir().toFile());
        assertThat(loader.getLaunchArguments(false)).containsExactly(
                "--username", "Jerry", "--demo", "--accessToken", "secret");
        assertThat(loader.getLaunchArguments(true)).containsExactly("--demo");
    }
}
