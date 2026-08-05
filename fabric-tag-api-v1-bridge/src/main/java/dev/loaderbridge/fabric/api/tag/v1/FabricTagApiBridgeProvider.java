package dev.loaderbridge.fabric.api.tag.v1;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class FabricTagApiBridgeProvider implements RuntimeBridgeModuleProvider {
    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            "fabric-tag-api-v1-bridge",
            "fabric-tag-api-v1:1.3.0",
            "1.3.0+1eb36c0719-loaderbridge.1",
            BridgeCapability.FABRIC_API,
            Set.of("net.fabricmc.fabric.api.tag.v1.FabricTagFile"),
            Map.of("fabric-tag-api-v1", "1.3.0+1eb36c0719"),
            Set.of("fabric-api-base-bridge", "fabric-resource-loader-v0-bridge"));

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
