package dev.loaderbridge.fixture.api;

import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.util.TriState;

/** Controlled runtime proof for automatically selected Fabric API base support. */
public final class FabricApiBaseFixture implements ModInitializer {
    @Override
    public void onInitialize() {
        AtomicInteger calls = new AtomicInteger();
        Event<Runnable> event = EventFactory.createArrayBacked(Runnable.class,
                listeners -> () -> {
                    for (Runnable listener : listeners) listener.run();
                });
        event.register(() -> calls.addAndGet(1));
        event.register(() -> calls.addAndGet(2));
        event.invoker().run();
        if (calls.get() != 3 || !TriState.DEFAULT.orElse(true)) {
            throw new IllegalStateException("Fabric API base bridge contract failed");
        }
        System.out.println("LOADERBRIDGE_FABRIC_API_BASE_READY");
    }
}
