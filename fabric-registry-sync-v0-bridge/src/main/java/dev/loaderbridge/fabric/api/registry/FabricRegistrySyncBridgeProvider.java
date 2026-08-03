package dev.loaderbridge.fabric.api.registry;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/** Advertises the version-pinned registry API surface implemented by this revision. */
public final class FabricRegistrySyncBridgeProvider implements RuntimeBridgeModuleProvider {
    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            "fabric-registry-sync-v0-bridge",
            "fabric-registry-sync-v0:5.1.3",
            "5.1.3+60c3209b19-loaderbridge.5",
            BridgeCapability.FABRIC_API,
            Set.of(
                    "net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder",
                    "net.fabricmc.fabric.api.event.registry.DynamicRegistries",
                    "net.fabricmc.fabric.api.event.registry.DynamicRegistries$SyncOption",
                    "net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback",
                    "net.fabricmc.fabric.api.event.registry.DynamicRegistryView",
                    "net.fabricmc.fabric.api.event.registry.RegistryAttribute",
                    "net.fabricmc.fabric.api.event.registry.RegistryAttributeHolder",
                    "net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback",
                    "net.fabricmc.fabric.api.event.registry.RegistryIdRemapCallback",
                    "net.fabricmc.fabric.api.event.registry.RegistryIdRemapCallback$RemapState"),
            Map.of("fabric-registry-sync-v0", "5.1.3+60c3209b19"),
            Set.of("fabric-api-base-bridge"));

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
