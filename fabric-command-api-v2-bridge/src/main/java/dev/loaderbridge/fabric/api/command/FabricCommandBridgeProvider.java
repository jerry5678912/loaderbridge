package dev.loaderbridge.fabric.api.command;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/** Advertises the exact server command callback surface implemented by this release. */
public final class FabricCommandBridgeProvider implements RuntimeBridgeModuleProvider {
    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            "fabric-command-api-v2-bridge",
            "fabric-command-api-v2:2.2.28",
            "2.2.28+6ced4dd919-loaderbridge.1",
            BridgeCapability.FABRIC_API,
            Set.of("net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback"),
            Map.of("fabric-command-api-v2", "2.2.28+6ced4dd919"),
            Set.of("fabric-api-base-bridge"));

    @Override
    public RuntimeBridgeModule descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public Path artifact() throws IOException {
        try {
            Path location = Path.of(FabricCommandBridgeProvider.class.getProtectionDomain()
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
