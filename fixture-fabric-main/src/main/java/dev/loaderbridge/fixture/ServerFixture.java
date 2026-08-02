package dev.loaderbridge.fixture;

import net.fabricmc.api.DedicatedServerModInitializer;

public final class ServerFixture implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        System.out.println("LOADERBRIDGE_FIXTURE_SERVER_READY");
    }
}
