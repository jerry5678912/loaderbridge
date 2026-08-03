package dev.loaderbridge.fabric.api.lifecycle;

import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.fml.common.Mod;

/** Connects Forge tick events to the binary-compatible Fabric callbacks. */
@Mod("loaderbridge_fabric_lifecycle")
public final class FabricLifecycleBridgeMod {
    public FabricLifecycleBridgeMod() {
        MinecraftForge.EVENT_BUS.addListener(this::onServerTickStart);
        MinecraftForge.EVENT_BUS.addListener(this::onServerTickEnd);
        MinecraftForge.EVENT_BUS.addListener(this::onLevelTickStart);
        MinecraftForge.EVENT_BUS.addListener(this::onLevelTickEnd);
        MinecraftForge.EVENT_BUS.addListener(this::onTagsUpdated);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopped);
        MinecraftForge.EVENT_BUS.addListener(this::onDataPackSync);
        MinecraftForge.EVENT_BUS.addListener(this::onLevelLoad);
        MinecraftForge.EVENT_BUS.addListener(this::onLevelUnload);
        MinecraftForge.EVENT_BUS.addListener(this::onEquipmentChange);
    }

    private void onServerTickStart(TickEvent.ServerTickEvent.Pre event) {
        ServerTickEvents.START_SERVER_TICK.invoker().onStartTick(event.getServer());
    }

    private void onServerTickEnd(TickEvent.ServerTickEvent.Post event) {
        ServerTickEvents.END_SERVER_TICK.invoker().onEndTick(event.getServer());
    }

    private void onLevelTickStart(TickEvent.LevelTickEvent.Pre event) {
        if (event.level instanceof ServerLevel serverLevel) {
            ServerTickEvents.START_WORLD_TICK.invoker().onStartTick(serverLevel);
        }
    }

    private void onLevelTickEnd(TickEvent.LevelTickEvent.Post event) {
        if (event.level instanceof ServerLevel serverLevel) {
            ServerTickEvents.END_WORLD_TICK.invoker().onEndTick(serverLevel);
        }
    }

    private void onTagsUpdated(TagsUpdatedEvent event) {
        boolean client = event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED;
        CommonLifecycleEvents.TAGS_LOADED.invoker().onTagsLoaded(event.getRegistryAccess(), client);
    }

    private void onServerStarting(ServerAboutToStartEvent event) {
        ServerLifecycleEvents.SERVER_STARTING.invoker().onServerStarting(event.getServer());
    }

    private void onServerStarted(ServerStartedEvent event) {
        ServerLifecycleEvents.SERVER_STARTED.invoker().onServerStarted(event.getServer());
    }

    private void onServerStopping(ServerStoppingEvent event) {
        ServerLifecycleEvents.SERVER_STOPPING.invoker().onServerStopping(event.getServer());
    }

    private void onServerStopped(ServerStoppedEvent event) {
        ServerLifecycleEvents.SERVER_STOPPED.invoker().onServerStopped(event.getServer());
    }

    private void onDataPackSync(OnDatapackSyncEvent event) {
        boolean joined = event.getPlayer() != null;
        dispatchDataPackSync(event.getPlayers(), joined);
    }

    static void dispatchDataPackSync(Iterable<net.minecraft.server.level.ServerPlayer> players,
            boolean joined) {
        for (var player : players) {
            ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.invoker()
                    .onSyncDataPackContents(player, joined);
        }
    }

    private void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && level.getServer() != null) {
            ServerWorldEvents.LOAD.invoker().onWorldLoad(level.getServer(), level);
        }
    }

    private void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level && level.getServer() != null) {
            ServerWorldEvents.UNLOAD.invoker().onWorldUnload(level.getServer(), level);
        }
    }

    private void onEquipmentChange(LivingEquipmentChangeEvent event) {
        ServerEntityEvents.EQUIPMENT_CHANGE.invoker().onChange(event.getEntity(), event.getSlot(),
                event.getFrom(), event.getTo());
    }
}
