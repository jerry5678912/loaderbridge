package dev.loaderbridge.fabric.api.biome;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class FabricBiomeBridgeProvider implements RuntimeBridgeModuleProvider {
    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            "fabric-biome-api-v1-bridge", "fabric-biome-api-v1:13.0.31",
            "13.0.31+d527f9fd19-loaderbridge.4", BridgeCapability.FABRIC_API,
            Set.of("net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext",
                    "net.fabricmc.fabric.api.biome.v1.BiomeSelectors",
                    "net.fabricmc.fabric.api.biome.v1.BiomeModifications",
                    "net.fabricmc.fabric.api.biome.v1.BiomeModification",
                    "net.fabricmc.fabric.api.biome.v1.BiomeModificationContext",
                    "net.fabricmc.fabric.api.biome.v1.BiomeModificationContext$WeatherContext",
                    "net.fabricmc.fabric.api.biome.v1.BiomeModificationContext$EffectsContext",
                    "net.fabricmc.fabric.api.biome.v1.BiomeModificationContext$GenerationSettingsContext",
                    "net.fabricmc.fabric.api.biome.v1.BiomeModificationContext$SpawnSettingsContext",
                    "net.fabricmc.fabric.api.biome.v1.ModificationPhase"),
            Map.of("fabric-biome-api-v1", "13.0.31+d527f9fd19"), Set.of());
    @Override public RuntimeBridgeModule descriptor() { return DESCRIPTOR; }
    @Override public Path artifact() throws IOException {
        try {
            Path path = Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!Files.isRegularFile(path)) throw new IOException("LB-MODULE-002: not running from a JAR");
            return path;
        } catch (URISyntaxException exception) { throw new IOException(exception); }
    }
}
