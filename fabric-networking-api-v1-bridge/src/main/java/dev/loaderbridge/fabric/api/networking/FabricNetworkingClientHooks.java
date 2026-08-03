package dev.loaderbridge.fabric.api.networking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;

/** Client-only event linkage kept out of the common mod entrypoint's signatures. */
final class FabricNetworkingClientHooks {
    static void register() {
        MinecraftForge.EVENT_BUS.addListener(FabricNetworkingClientHooks::onLogout);
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientPlayNetworking.clearLocalReceivers();
    }

    private FabricNetworkingClientHooks() { }
}
