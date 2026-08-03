package dev.loaderbridge.fixture;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class MainFixture implements ModInitializer {
    @Override
    public void onInitialize() {
        var loader = FabricLoader.getInstance();
        var custom = loader.getEntrypointContainers(
                "loaderbridge:fixture_api", FixtureApi.class);
        if (custom.size() != 1
                || !custom.getFirst().getProvider().getMetadata().getId()
                        .equals("loaderbridge_fixture")
                || !custom.getFirst().getDefinition()
                        .equals("dev.loaderbridge.fixture.FixtureApiProvider")
                || custom.getFirst().getEntrypoint() != custom.getFirst().getEntrypoint()
                || !custom.getFirst().getEntrypoint().value().equals("custom-entrypoint")) {
            throw new IllegalStateException("custom Fabric entrypoint contract failed");
        }
        if (!loader.getRawGameVersion().equals("1.21.1")) {
            throw new IllegalStateException(
                    "unexpected raw game version: " + loader.getRawGameVersion());
        }
        System.out.println("LOADERBRIDGE_FIXTURE_CUSTOM_ENTRYPOINT_READY");
        System.out.println("LOADERBRIDGE_FIXTURE_RAW_GAME_VERSION=1.21.1");
        System.out.println("LOADERBRIDGE_FIXTURE_MAIN_READY");
    }
}
