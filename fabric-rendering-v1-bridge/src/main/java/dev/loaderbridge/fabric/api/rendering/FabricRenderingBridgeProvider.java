package dev.loaderbridge.fabric.api.rendering;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class FabricRenderingBridgeProvider implements RuntimeBridgeModuleProvider {
    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            "fabric-rendering-v1-bridge", "fabric-rendering-v1:5.1.0",
            "5.1.0+ab4c25a019-loaderbridge.3", BridgeCapability.FABRIC_API,
            Set.of("net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry",
                    "net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry",
                    "net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry",
                    "net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry",
                    "net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry$TexturedModelDataProvider"),
            Map.of("fabric-rendering-v1", "5.1.0+ab4c25a019"), Set.of("fabric-api-base-bridge"));
    @Override public RuntimeBridgeModule descriptor() { return DESCRIPTOR; }
    @Override public Path artifact() throws IOException {
        try {
            Path path = Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!Files.isRegularFile(path)) throw new IOException("LB-MODULE-002: not running from a JAR");
            return path;
        } catch (URISyntaxException exception) { throw new IOException(exception); }
    }
}
