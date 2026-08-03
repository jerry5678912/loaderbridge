package dev.loaderbridge.fabric.api.networking;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class FabricNetworkingBridgeProvider implements RuntimeBridgeModuleProvider {
    private static final Set<String> PUBLIC_TYPES = Set.of(
            "net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents",
            "net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents$StartTracking",
            "net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents$StopTracking",
            "net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking",
            "net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking$Context",
            "net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking$PlayPayloadHandler",
            "net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents",
            "net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents$Complete",
            "net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents$Disconnect",
            "net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents$Init",
            "net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents$Ready",
            "net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents$Start",
            "net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking",
            "net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking$ConfigurationPayloadHandler",
            "net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking$Context",
            "net.fabricmc.fabric.api.networking.v1.PacketByteBufs",
            "net.fabricmc.fabric.api.networking.v1.PacketSender",
            "net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry",
            "net.fabricmc.fabric.api.networking.v1.PlayerLookup",
            "net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents",
            "net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents$Configure",
            "net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents$Disconnect",
            "net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking",
            "net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking$ConfigurationPacketHandler",
            "net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking$Context",
            "net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents",
            "net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents$Disconnect",
            "net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents$Init",
            "net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents$Join",
            "net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking",
            "net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking$Context",
            "net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking$PlayPayloadHandler");

    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            "fabric-networking-api-v1-bridge", "fabric-networking-api-v1:4.3.1",
            "4.3.1+d30f6a7919-loaderbridge.4", BridgeCapability.FABRIC_API,
            PUBLIC_TYPES, Map.of("fabric-networking-api-v1", "4.3.1+d30f6a7919"),
            Set.of("fabric-api-base-bridge"));

    @Override public RuntimeBridgeModule descriptor() { return DESCRIPTOR; }

    @Override public Path artifact() throws IOException {
        try {
            Path path = Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!Files.isRegularFile(path)) throw new IOException("LB-MODULE-002: not running from a JAR");
            return path;
        } catch (URISyntaxException exception) {
            throw new IOException(exception);
        }
    }
}
