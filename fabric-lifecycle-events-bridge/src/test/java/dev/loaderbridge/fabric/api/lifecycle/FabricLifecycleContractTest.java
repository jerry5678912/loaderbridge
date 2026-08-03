package dev.loaderbridge.fabric.api.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
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
        assertThat(descriptor.implementationVersion())
                .isEqualTo("2.6.0+0865547519-loaderbridge.5");
        assertThat(descriptor.providedModVersions())
                .containsEntry("fabric-lifecycle-events-v1", "2.6.0+0865547519");
        assertThat(descriptor.requiredModules()).containsExactly("fabric-api-base-bridge");
        assertThat(descriptor.providedClasses()).contains(
                "net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents",
                "net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents$Unload",
                "net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents",
                "net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents$TagsLoaded",
                "net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents",
                "net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents$AfterSave",
                "net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents",
                "net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents$EquipmentChange",
                "net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents",
                "net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents$Unload",
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

    @Test
    void serverLifecycleEventsPreserveOrderArgumentsAndReloadOutcome() {
        List<String> calls = new ArrayList<>();
        ServerLifecycleEvents.SERVER_STARTING.register(server -> calls.add("starting"));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> calls.add("started"));
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register(
                (server, resources) -> calls.add("reload-start"));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(
                (server, resources, success) -> calls.add("reload-end:" + success));
        ServerLifecycleEvents.BEFORE_SAVE.register(
                (server, flush, force) -> calls.add("save-start:" + flush + ":" + force));
        ServerLifecycleEvents.AFTER_SAVE.register(
                (server, flush, force) -> calls.add("save-end:" + flush + ":" + force));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> calls.add("stopping"));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> calls.add("stopped"));

        ServerLifecycleEvents.SERVER_STARTING.invoker().onServerStarting(null);
        ServerLifecycleEvents.SERVER_STARTED.invoker().onServerStarted(null);
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.invoker().startDataPackReload(null, null);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.invoker().endDataPackReload(null, null, true);
        ServerLifecycleEvents.BEFORE_SAVE.invoker().onBeforeSave(null, true, false);
        ServerLifecycleEvents.AFTER_SAVE.invoker().onAfterSave(null, true, false);
        ServerLifecycleEvents.SERVER_STOPPING.invoker().onServerStopping(null);
        ServerLifecycleEvents.SERVER_STOPPED.invoker().onServerStopped(null);

        assertThat(calls).containsExactly("starting", "started", "reload-start", "reload-end:true",
                "save-start:true:false", "save-end:true:false", "stopping", "stopped");
    }

    @Test
    void datapackSyncDispatchPreservesJoinedFlagForEverySelectedPlayer() {
        List<Boolean> joinedValues = new ArrayList<>();
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register(
                (player, joined) -> joinedValues.add(joined));

        FabricLifecycleBridgeMod.dispatchDataPackSync(
                java.util.Arrays.asList(null, null), false);
        FabricLifecycleBridgeMod.dispatchDataPackSync(
                java.util.Collections.singletonList(null), true);

        assertThat(joinedValues).containsExactly(false, false, true);
    }

    @Test
    void serverWorldEventsPreserveLoadAndUnloadOrder() {
        List<String> calls = new ArrayList<>();
        ServerWorldEvents.LOAD.register((server, world) -> calls.add("load"));
        ServerWorldEvents.UNLOAD.register((server, world) -> calls.add("unload"));

        ServerWorldEvents.LOAD.invoker().onWorldLoad(null, null);
        ServerWorldEvents.UNLOAD.invoker().onWorldUnload(null, null);

        assertThat(calls).containsExactly("load", "unload");
    }

    @Test
    void serverEntityEventsPreserveLoadUnloadAndEquipmentArguments() {
        List<String> calls = new ArrayList<>();
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> calls.add("load"));
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> calls.add("unload"));
        ServerEntityEvents.EQUIPMENT_CHANGE.register(
                (entity, slot, previous, current) -> calls.add("equipment"));

        ServerEntityEvents.ENTITY_LOAD.invoker().onLoad(null, null);
        ServerEntityEvents.EQUIPMENT_CHANGE.invoker().onChange(null, null, null, null);
        ServerEntityEvents.ENTITY_UNLOAD.invoker().onUnload(null, null);

        assertThat(calls).containsExactly("load", "equipment", "unload");
    }

    @Test
    void serverBlockEntityEventsPreserveLoadAndUnloadOrder() {
        List<String> calls = new ArrayList<>();
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((entity, world) -> calls.add("load"));
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((entity, world) -> calls.add("unload"));

        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.invoker().onLoad(null, null);
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.invoker().onUnload(null, null);

        assertThat(calls).containsExactly("load", "unload");
    }
}
