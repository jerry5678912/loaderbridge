package dev.loaderbridge.fabric.api.screenhandler;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/** Advertises the complete pinned Fabric Screen Handler API v1 contract. */
public final class FabricScreenHandlerBridgeProvider implements RuntimeBridgeModuleProvider {
    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            "fabric-screen-handler-api-v1-bridge",
            "fabric-screen-handler-api-v1:1.3.91",
            "1.3.91+b559734419-loaderbridge.1",
            BridgeCapability.FABRIC_API,
            Set.of(
                    "net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory",
                    "net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType",
                    "net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType$ExtendedFactory",
                    "net.fabricmc.fabric.api.screenhandler.v1.FabricScreenHandlerFactory"),
            Map.of("fabric-screen-handler-api-v1", "1.3.91+b559734419"),
            Set.of("fabric-api-base-bridge", "fabric-networking-api-v1-bridge"));

    @Override
    public RuntimeBridgeModule descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public Path artifact() throws IOException {
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
