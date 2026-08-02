package dev.loaderbridge.fixture;

import net.fabricmc.api.ClientModInitializer;

public final class ClientFixture implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println("LOADERBRIDGE_FIXTURE_CLIENT_READY");
    }
}
