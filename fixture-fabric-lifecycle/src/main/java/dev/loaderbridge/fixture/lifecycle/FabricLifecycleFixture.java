package dev.loaderbridge.fixture.lifecycle;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/** Verifies Forge-to-Fabric server and world tick ordering at runtime. */
public final class FabricLifecycleFixture implements ModInitializer {
    private static final AtomicInteger STATE = new AtomicInteger();
    private static final AtomicBoolean REPORTED = new AtomicBoolean();

    @Override
    public void onInitialize() {
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
            if (!client) {
                System.out.println("LOADERBRIDGE_FABRIC_LIFECYCLE_TAGS_READY");
            }
        });
        ServerTickEvents.START_SERVER_TICK.register(server -> STATE.compareAndSet(0, 1));
        ServerTickEvents.START_WORLD_TICK.register(world -> STATE.compareAndSet(1, 2));
        ServerTickEvents.END_WORLD_TICK.register(world -> STATE.compareAndSet(2, 3));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (STATE.compareAndSet(3, 4) && REPORTED.compareAndSet(false, true)) {
                System.out.println("LOADERBRIDGE_FABRIC_LIFECYCLE_TICKS_READY");
            }
        });
    }
}
