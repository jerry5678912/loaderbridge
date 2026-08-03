package dev.loaderbridge.fixture;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public final class PreLaunchFixture implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        var custom = FabricLoader.getInstance().getEntrypointContainers(
                "loaderbridge:fixture_api", FixtureApi.class);
        if (custom.size() != 1
                || !custom.getFirst().getEntrypoint().value().equals("custom-entrypoint")) {
            throw new IllegalStateException(
                    "custom entrypoints were not available during preLaunch");
        }
        System.out.println("LOADERBRIDGE_FIXTURE_PRELAUNCH_DISCOVERY_READY");
        System.out.println("LOADERBRIDGE_FIXTURE_PRELAUNCH_READY");
    }
}
