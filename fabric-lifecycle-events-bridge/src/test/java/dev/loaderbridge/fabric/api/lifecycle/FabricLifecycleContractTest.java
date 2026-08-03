package dev.loaderbridge.fabric.api.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.junit.jupiter.api.Test;

class FabricLifecycleContractTest {
    @Test
    void serverAndWorldTickEventsInvokeListenersInRegistrationOrder() {
        List<String> calls = new ArrayList<>();
        ServerTickEvents.START_SERVER_TICK.register(server -> calls.add("server-start-first"));
        ServerTickEvents.START_SERVER_TICK.register(server -> calls.add("server-start-second"));
        ServerTickEvents.START_WORLD_TICK.register(world -> calls.add("world-start"));
        ServerTickEvents.END_WORLD_TICK.register(world -> calls.add("world-end"));
        ServerTickEvents.END_SERVER_TICK.register(server -> calls.add("server-end"));

        ServerTickEvents.START_SERVER_TICK.invoker().onStartTick(null);
        ServerTickEvents.START_WORLD_TICK.invoker().onStartTick(null);
        ServerTickEvents.END_WORLD_TICK.invoker().onEndTick(null);
        ServerTickEvents.END_SERVER_TICK.invoker().onEndTick(null);

        assertThat(calls).containsExactly(
                "server-start-first", "server-start-second", "world-start", "world-end", "server-end");
    }

    @Test
    void providerPinsExactFabricContractAndBaseRequirement() {
        var descriptor = new FabricLifecycleBridgeProvider().descriptor();

        assertThat(descriptor.contractVersion()).isEqualTo("fabric-lifecycle-events-v1:2.6.0");
        assertThat(descriptor.providedModVersions())
                .containsEntry("fabric-lifecycle-events-v1", "2.6.0+0865547519");
        assertThat(descriptor.requiredModules()).containsExactly("fabric-api-base-bridge");
        assertThat(descriptor.providedClasses()).contains(
                "net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents",
                "net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents$TagsLoaded",
                "net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents",
                "net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents$StartTick",
                "net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents$EndWorldTick");
    }

    @Test
    void commonTagsLoadedEventPreservesRegistryAndClientFlag() {
        List<String> calls = new ArrayList<>();
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) ->
                calls.add(registries + ":" + client));

        CommonLifecycleEvents.TAGS_LOADED.invoker().onTagsLoaded(null, false);
        CommonLifecycleEvents.TAGS_LOADED.invoker().onTagsLoaded(null, true);

        assertThat(calls).containsExactly("null:false", "null:true");
    }
}
