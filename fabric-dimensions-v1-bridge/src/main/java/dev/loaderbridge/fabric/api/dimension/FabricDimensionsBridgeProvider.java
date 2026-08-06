package dev.loaderbridge.fabric.api.dimension;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class FabricDimensionsBridgeProvider implements RuntimeBridgeModuleProvider {
    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            "fabric-dimensions-v1-bridge",
            "fabric-dimensions-v1:4.0.1",
            "4.0.1+65213ef819-loaderbridge.1",
            BridgeCapability.FABRIC_API,
            Set.of(),
            Map.of("fabric-dimensions-v1", "4.0.1+65213ef819"),
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
