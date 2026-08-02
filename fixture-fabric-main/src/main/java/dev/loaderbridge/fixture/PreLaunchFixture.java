package dev.loaderbridge.fixture;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public final class PreLaunchFixture implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        System.out.println("LOADERBRIDGE_FIXTURE_PRELAUNCH_READY");
    }
}
