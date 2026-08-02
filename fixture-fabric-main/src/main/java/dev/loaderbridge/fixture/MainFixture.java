package dev.loaderbridge.fixture;

import net.fabricmc.api.ModInitializer;

public final class MainFixture implements ModInitializer {
    @Override
    public void onInitialize() {
        System.out.println("LOADERBRIDGE_FIXTURE_MAIN_READY");
    }
}
