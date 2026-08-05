package dev.loaderbridge.fabric.api.interaction;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class FabricInteractionEventsBridgeProvider implements RuntimeBridgeModuleProvider {
    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            "fabric-events-interaction-v0-bridge",
            "fabric-events-interaction-v0:0.7.14",
            "0.7.14+ba9dae0619-loaderbridge.1",
            BridgeCapability.FABRIC_API,
            Set.of(
                    "net.fabricmc.fabric.api.block.BlockAttackInteractionAware",
                    "net.fabricmc.fabric.api.event.player.AttackBlockCallback",
                    "net.fabricmc.fabric.api.event.player.AttackEntityCallback",
                    "net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents",
                    "net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents$After",
                    "net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents$Before",
                    "net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents$Canceled",
                    "net.fabricmc.fabric.api.event.player.UseBlockCallback",
                    "net.fabricmc.fabric.api.event.player.UseEntityCallback",
                    "net.fabricmc.fabric.api.event.player.UseItemCallback"),
            Map.of("fabric-events-interaction-v0", "0.7.14+ba9dae0619"),
            Set.of("fabric-api-base-bridge"));

    @Override
    public RuntimeBridgeModule descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public Path artifact() throws IOException {
        try {
            Path location = Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!Files.isRegularFile(location) || !location.getFileName().toString().endsWith(".jar")) {
                throw new IOException("LB-MODULE-002: bridge module is not running from a JAR: " + location);
            }
            return location;
        } catch (URISyntaxException exception) {
            throw new IOException("LB-MODULE-002: invalid bridge module location", exception);
        }
    }
}
