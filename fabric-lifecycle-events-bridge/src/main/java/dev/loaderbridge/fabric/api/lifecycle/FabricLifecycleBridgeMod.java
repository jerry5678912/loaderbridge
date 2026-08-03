package dev.loaderbridge.fabric.api.lifecycle;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

/** Connects Forge tick events to the binary-compatible Fabric callbacks. */
@Mod("loaderbridge_fabric_lifecycle")
public final class FabricLifecycleBridgeMod {
    public FabricLifecycleBridgeMod() {
        MinecraftForge.EVENT_BUS.addListener(this::onServerTickStart);
        MinecraftForge.EVENT_BUS.addListener(this::onServerTickEnd);
        MinecraftForge.EVENT_BUS.addListener(this::onLevelTickStart);
        MinecraftForge.EVENT_BUS.addListener(this::onLevelTickEnd);
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
}
