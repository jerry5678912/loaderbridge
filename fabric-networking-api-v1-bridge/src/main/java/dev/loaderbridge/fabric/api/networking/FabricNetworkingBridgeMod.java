package dev.loaderbridge.fabric.api.networking;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod("loaderbridge_fabric_networking")
public final class FabricNetworkingBridgeMod {
    @SuppressWarnings("removal")
    public FabricNetworkingBridgeMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
        MinecraftForge.EVENT_BUS.addListener(this::onLogin);
        MinecraftForge.EVENT_BUS.addListener(this::onLogout);
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
            ServerPlayConnectionEvents.JOIN.invoker().onPlayReady(
                    handler, ServerPlayNetworking.getSender(handler), player.server);
        }
    }

    private void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerPlayConnectionEvents.DISCONNECT.invoker()
                    .onPlayDisconnect(player.connection, player.server);
            ServerPlayNetworking.clear(player.connection);
        }
    }

}
