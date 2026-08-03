package dev.loaderbridge.fabric.api.lifecycle;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/** Advertises the server tick surface implemented by this bridge release. */
public final class FabricLifecycleBridgeProvider implements RuntimeBridgeModuleProvider {
    private static final String MODULE_ID = "fabric-lifecycle-events-bridge";
    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            MODULE_ID,
            "fabric-lifecycle-events-v1:2.6.0",
            "2.6.0+0865547519-loaderbridge.5",
            BridgeCapability.FABRIC_API,
            Set.of(
                    "net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents",
                    "net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents$Load",
                    "net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents$Unload",
                    "net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents",
                    "net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents$TagsLoaded",
                    "net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents",
                    "net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents$Load",
                    "net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents$Unload",
                    "net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents$EquipmentChange",
                    "net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents",
                    "net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents$ServerStarting",
                    "net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents$ServerStarted",
                    "net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents$ServerStopping",
                    "net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents$ServerStopped",
                    "net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents$SyncDataPackContents",
                    "net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents$StartDataPackReload",
                    "net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents$EndDataPackReload",
                    "net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents$BeforeSave",
                    "net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents$AfterSave",
                    "net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents",
                    "net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents$StartTick",
                    "net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents$EndTick",
                    "net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents$StartWorldTick",
                    "net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents$EndWorldTick",
                    "net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents",
                    "net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents$Load",
                    "net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents$Unload"),
            Map.of("fabric-lifecycle-events-v1", "2.6.0+0865547519"),
            Set.of("fabric-api-base-bridge"));

    @Override
    public RuntimeBridgeModule descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public Path artifact() throws IOException {
        try {
            Path location = Path.of(FabricLifecycleBridgeProvider.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (!Files.isRegularFile(location) || !location.getFileName().toString().endsWith(".jar")) {
                throw new IOException("LB-MODULE-002: bridge module is not running from a JAR: " + location);
            }
            return location;
        } catch (URISyntaxException exception) {
            throw new IOException("LB-MODULE-002: invalid bridge module location", exception);
        }
    }
}
