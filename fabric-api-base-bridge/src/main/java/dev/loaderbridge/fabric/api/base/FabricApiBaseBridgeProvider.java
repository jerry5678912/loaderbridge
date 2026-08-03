package dev.loaderbridge.fabric.api.base;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.Map;

/** Advertises the exact Fabric API base surface implemented by this JAR. */
public final class FabricApiBaseBridgeProvider implements RuntimeBridgeModuleProvider {
    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            "fabric-api-base-bridge",
            "fabric-api-base:0.4.42",
            "0.4.42+6573ed8c19-loaderbridge.1",
            BridgeCapability.FABRIC_API,
            Set.of(
                    "net.fabricmc.fabric.api.event.AutoInvokingEvent",
                    "net.fabricmc.fabric.api.event.Event",
                    "net.fabricmc.fabric.api.event.EventFactory",
                    "net.fabricmc.fabric.api.util.BooleanFunction",
                    "net.fabricmc.fabric.api.util.NbtType",
                    "net.fabricmc.fabric.api.util.TriState"),
            Map.of("fabric-api-base", "0.4.42+6573ed8c19"),
            Set.of());

    @Override
    public RuntimeBridgeModule descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public Path artifact() throws IOException {
        try {
            Path location = Path.of(FabricApiBaseBridgeProvider.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (!Files.isRegularFile(location) || !location.getFileName().toString().endsWith(".jar")) {
                throw new IOException("LB-MODULE-002: bridge module is not running from a JAR: " + location);
            }
            return location;
        } catch (URISyntaxException exception) {
            throw new IOException("LB-MODULE-002: invalid bridge module location", exception);
        }
    }
}
