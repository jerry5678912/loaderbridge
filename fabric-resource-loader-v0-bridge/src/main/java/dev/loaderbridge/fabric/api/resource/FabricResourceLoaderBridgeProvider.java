package dev.loaderbridge.fabric.api.resource;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class FabricResourceLoaderBridgeProvider implements RuntimeBridgeModuleProvider {
    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            "fabric-resource-loader-v0-bridge", "fabric-resource-loader-v0:1.3.1",
            "1.3.1+5b5275af19-loaderbridge.2", BridgeCapability.FABRIC_API,
            Set.of("net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener",
                    "net.fabricmc.fabric.api.resource.ModResourcePack",
                    "net.fabricmc.fabric.api.resource.ResourceManagerHelper",
                    "net.fabricmc.fabric.api.resource.ResourcePackActivationType",
                    "net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys",
                    "net.fabricmc.fabric.api.resource.SimpleResourceReloadListener",
                    "net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener"),
            Map.of("fabric-resource-loader-v0", "1.3.1+5b5275af19"),
            Set.of("fabric-api-base-bridge"));

    @Override public RuntimeBridgeModule descriptor() { return DESCRIPTOR; }

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
