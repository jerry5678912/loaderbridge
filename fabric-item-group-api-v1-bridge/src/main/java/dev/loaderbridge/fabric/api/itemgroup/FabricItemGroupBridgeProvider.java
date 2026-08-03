package dev.loaderbridge.fabric.api.itemgroup;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class FabricItemGroupBridgeProvider implements RuntimeBridgeModuleProvider {
    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            "fabric-item-group-api-v1-bridge", "fabric-item-group-api-v1:4.1.7",
            "4.1.7+def88e3a19-loaderbridge.1", BridgeCapability.FABRIC_API,
            Set.of("net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup",
                    "net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries",
                    "net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents",
                    "net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents$ModifyEntries",
                    "net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents$ModifyEntriesAll"),
            Map.of("fabric-item-group-api-v1", "4.1.7+def88e3a19"),
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
