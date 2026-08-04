package dev.loaderbridge.fabric.api.loot;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class FabricLootApiV3BridgeProvider implements RuntimeBridgeModuleProvider {
    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            "fabric-loot-api-v3-bridge",
            "fabric-loot-api-v3:1.0.3",
            "1.0.3+3f89f5a519-loaderbridge.1",
            BridgeCapability.FABRIC_API,
            Set.of(
                    "net.fabricmc.fabric.api.loot.v3.FabricLootPoolBuilder",
                    "net.fabricmc.fabric.api.loot.v3.FabricLootTableBuilder",
                    "net.fabricmc.fabric.api.loot.v3.LootTableEvents",
                    "net.fabricmc.fabric.api.loot.v3.LootTableEvents$Replace",
                    "net.fabricmc.fabric.api.loot.v3.LootTableEvents$Modify",
                    "net.fabricmc.fabric.api.loot.v3.LootTableEvents$Loaded",
                    "net.fabricmc.fabric.api.loot.v3.LootTableSource"),
            Map.of("fabric-loot-api-v3", "1.0.3+3f89f5a519"),
            Set.of("fabric-api-base-bridge", "fabric-resource-loader-v0-bridge"));

    @Override public RuntimeBridgeModule descriptor() { return DESCRIPTOR; }

    @Override public Path artifact() throws IOException {
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
