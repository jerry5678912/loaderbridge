package dev.loaderbridge.fabric.api.networking;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraftforge.event.TickEvent;

@Mod("loaderbridge_fabric_networking")
public final class FabricNetworkingBridgeMod {
    private final ConcurrentLinkedQueue<ServerPlayer> pendingJoins = new ConcurrentLinkedQueue<>();
    @SuppressWarnings("removal")
    public FabricNetworkingBridgeMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
        MinecraftForge.EVENT_BUS.addListener(this::onLogin);
        MinecraftForge.EVENT_BUS.addListener(this::onLogout);
        MinecraftForge.EVENT_BUS.addListener(this::onStartTracking);
        MinecraftForge.EVENT_BUS.addListener(this::onStopTracking);
        MinecraftForge.EVENT_BUS.addListener(this::onServerTickEnd);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            FabricNetworkingClientHooks.register();
        }
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkBridgeRuntime::finalizeRegistrations);
    }

    private void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var handler = player.connection;
            ServerPlayConnectionEvents.INIT.invoker().onPlayInit(handler, player.server);
            pendingJoins.add(player);
        }
    }

    private void onServerTickEnd(TickEvent.ServerTickEvent.Post event) {
        ServerPlayer player;
        while ((player = pendingJoins.poll()) != null) {
            var handler = player.connection;
            if (handler.isAcceptingMessages()) {
                ServerPlayConnectionEvents.JOIN.invoker().onPlayReady(
                        handler, ServerPlayNetworking.getSender(handler), event.getServer());
            }
        }
    }

    private void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            pendingJoins.remove(player);
            ServerPlayConnectionEvents.DISCONNECT.invoker()
                    .onPlayDisconnect(player.connection, player.server);
            ServerPlayNetworking.clear(player.connection);
        }
    }

    private void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EntityTrackingEvents.START_TRACKING.invoker().onStartTracking(event.getTarget(), player);
        }
    }

    private void onStopTracking(PlayerEvent.StopTracking event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EntityTrackingEvents.STOP_TRACKING.invoker().onStopTracking(event.getTarget(), player);
        }
    }

}
