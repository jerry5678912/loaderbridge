package dev.loaderbridge.fabric.api.lookup;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/** Advertises the exact block lookup surface implemented by this revision. */
public final class FabricApiLookupBridgeProvider implements RuntimeBridgeModuleProvider {
    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            "fabric-api-lookup-api-v1-bridge",
            "fabric-api-lookup-api-v1:1.6.72",
            "1.6.72+d30f6a7919-loaderbridge.1",
            BridgeCapability.FABRIC_API,
            Set.of(
                    "net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup",
                    "net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup$BlockApiProvider",
                    "net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup$BlockEntityApiProvider",
                    "net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache"),
            Map.of("fabric-api-lookup-api-v1", "1.6.72+d30f6a7919"),
            Set.of("fabric-api-base-bridge", "fabric-lifecycle-events-bridge"));

    @Override public RuntimeBridgeModule descriptor() { return DESCRIPTOR; }

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
