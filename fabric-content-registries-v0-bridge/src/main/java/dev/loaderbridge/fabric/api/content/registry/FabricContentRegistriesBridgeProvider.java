package dev.loaderbridge.fabric.api.content.registry;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class FabricContentRegistriesBridgeProvider implements RuntimeBridgeModuleProvider {
    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            "fabric-content-registries-v0-bridge",
            "fabric-content-registries-v0:8.0.19",
            "8.0.19+b559734419-loaderbridge.2",
            BridgeCapability.FABRIC_API,
            Set.of(
                    "net.fabricmc.fabric.api.util.Item2ObjectMap",
                    "net.fabricmc.fabric.api.util.Block2ObjectMap",
                    "net.fabricmc.fabric.api.registry.FuelRegistry",
                    "net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder",
                    "net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder$BuildCallback",
                    "net.fabricmc.fabric.api.registry.CompostingChanceRegistry",
                    "net.fabricmc.fabric.api.registry.FlammableBlockRegistry",
                    "net.fabricmc.fabric.api.registry.FlammableBlockRegistry$Entry",
                    "net.fabricmc.fabric.api.registry.FlattenableBlockRegistry",
                    "net.fabricmc.fabric.api.registry.StrippableBlockRegistry",
                    "net.fabricmc.fabric.api.registry.OxidizableBlocksRegistry"),
            Map.of("fabric-content-registries-v0", "8.0.19+b559734419"),
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
