package dev.loaderbridge.fabric.api.block;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class FabricBlockApiV1BridgeProvider implements RuntimeBridgeModuleProvider {
    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            "fabric-block-api-v1-bridge",
            "fabric-block-api-v1:1.1.0",
            "1.1.0+0bc3503219-loaderbridge.1",
            BridgeCapability.FABRIC_API,
            Set.of(
                    "net.fabricmc.fabric.api.block.v1.BlockFunctionalityTags",
                    "net.fabricmc.fabric.api.block.v1.FabricBlock",
                    "net.fabricmc.fabric.api.block.v1.FabricBlockState"),
            Map.of("fabric-block-api-v1", "1.1.0+0bc3503219"),
            Set.of());

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
