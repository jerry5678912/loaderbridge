package dev.loaderbridge.fixture.lifecycle;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class FabricNetworkingClientFixture implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FabricNetworkingPayload.PING_TYPE,
                (payload, context) -> {
                    if (payload.value().equals("ping")) {
                        System.out.println("LOADERBRIDGE_FABRIC_NETWORK_CLIENT_RECEIVED");
                        ClientPlayNetworking.send(new FabricNetworkingPayload(
                                FabricNetworkingPayload.PONG_TYPE, "pong"));
                    }
                });
    }
}
