package dev.loaderbridge.fabric.api.blockrenderlayer;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class FabricBlockRenderLayerBridgeProvider implements RuntimeBridgeModuleProvider {
    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            "fabric-blockrenderlayer-v1-bridge", "fabric-blockrenderlayer-v1:1.1.52",
            "1.1.52+0af3f5a719-loaderbridge.1", BridgeCapability.FABRIC_API,
            Set.of("net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap"),
            Map.of("fabric-blockrenderlayer-v1", "1.1.52+0af3f5a719"), Set.of());
    @Override public RuntimeBridgeModule descriptor() { return DESCRIPTOR; }
    @Override public Path artifact() throws IOException {
        try {
            Path path = Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!Files.isRegularFile(path)) throw new IOException("LB-MODULE-002: not running from a JAR");
            return path;
        } catch (URISyntaxException exception) { throw new IOException(exception); }
    }
}
