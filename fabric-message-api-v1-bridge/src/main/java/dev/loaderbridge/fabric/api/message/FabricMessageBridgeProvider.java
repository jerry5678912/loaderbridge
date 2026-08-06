package dev.loaderbridge.fabric.api.message;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/** Advertises the pinned common/server Message API v1 surface. */
public final class FabricMessageBridgeProvider implements RuntimeBridgeModuleProvider {
    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            "fabric-message-api-v1-bridge",
            "fabric-message-api-v1:6.0.14",
            "6.0.14+6ced4dd919-loaderbridge.1",
            BridgeCapability.FABRIC_API,
            Set.of(
                    "net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent",
                    "net.fabricmc.fabric.api.message.v1.ServerMessageEvents",
                    "net.fabricmc.fabric.api.message.v1.ServerMessageEvents$AllowChatMessage",
                    "net.fabricmc.fabric.api.message.v1.ServerMessageEvents$AllowGameMessage",
                    "net.fabricmc.fabric.api.message.v1.ServerMessageEvents$AllowCommandMessage",
                    "net.fabricmc.fabric.api.message.v1.ServerMessageEvents$ChatMessage",
                    "net.fabricmc.fabric.api.message.v1.ServerMessageEvents$GameMessage",
                    "net.fabricmc.fabric.api.message.v1.ServerMessageEvents$CommandMessage"),
            Map.of("fabric-message-api-v1", "6.0.14+6ced4dd919"),
            Set.of("fabric-api-base-bridge"));

    @Override public RuntimeBridgeModule descriptor() { return DESCRIPTOR; }

    @Override public Path artifact() throws IOException {
        try {
            Path path = Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!Files.isRegularFile(path) || !path.getFileName().toString().endsWith(".jar")) {
                throw new IOException("LB-MODULE-002: bridge module is not running from a JAR: " + path);
            }
            return path;
        } catch (URISyntaxException exception) {
            throw new IOException("LB-MODULE-002: invalid bridge module location", exception);
        }
    }
}
