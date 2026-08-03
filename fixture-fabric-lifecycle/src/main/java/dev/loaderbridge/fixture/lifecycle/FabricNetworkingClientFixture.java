package dev.loaderbridge.fixture.lifecycle;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;

public final class FabricNetworkingClientFixture implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientConfigurationConnectionEvents.INIT.register((handler, client) ->
                System.out.println("LOADERBRIDGE_FABRIC_CLIENT_CONFIG_INIT"));
        ClientConfigurationConnectionEvents.START.register((handler, client) -> {
            if (!ClientConfigurationNetworking.canSend(FabricNetworkingPayload.CONFIG_PONG_TYPE)) {
                throw new IllegalStateException("LOADERBRIDGE_FABRIC_CONFIG_CLIENT_CANNOT_SEND");
            }
            System.out.println("LOADERBRIDGE_FABRIC_CLIENT_CONFIG_START");
        });
        ClientConfigurationConnectionEvents.COMPLETE.register((handler, client) ->
                System.out.println("LOADERBRIDGE_FABRIC_CLIENT_CONFIG_COMPLETE"));
        ClientConfigurationNetworking.registerGlobalReceiver(
                FabricNetworkingPayload.CONFIG_PING_TYPE, (payload, context) -> {
                    if (payload.value().equals("config_ping")) {
                        System.out.println("LOADERBRIDGE_FABRIC_CONFIG_CLIENT_RECEIVED");
                        context.responseSender().sendPacket(new FabricNetworkingPayload(
                                FabricNetworkingPayload.CONFIG_PONG_TYPE, "config_pong"));
                    }
                });
        ClientPlayNetworking.registerGlobalReceiver(FabricNetworkingPayload.PING_TYPE,
                (payload, context) -> {
                    if (payload.value().equals("ping")) {
                        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(
                                FabricLifecycleFixture.ITEM_GROUP_KEY.location());
                        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(
                                context.player().level().enabledFeatures(),
                                context.player().hasPermissions(2),
                                context.player().registryAccess()));
                        System.out.println("LOADERBRIDGE_FABRIC_NETWORK_CLIENT_RECEIVED");
                        ClientPlayNetworking.send(new FabricNetworkingPayload(
                                FabricNetworkingPayload.PONG_TYPE, "pong"));
                    }
                });
    }
}
