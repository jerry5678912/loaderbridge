package dev.loaderbridge.fabric.api.resource.conditions;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class FabricResourceConditionsBridgeProvider implements RuntimeBridgeModuleProvider {
    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            "fabric-resource-conditions-api-v1-bridge",
            "fabric-resource-conditions-api-v1:4.3.0",
            "4.3.0+8dc279b119-loaderbridge.1",
            BridgeCapability.FABRIC_API,
            Set.of(
                    "net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition",
                    "net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType",
                    "net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions"),
            Map.of("fabric-resource-conditions-api-v1", "4.3.0+8dc279b119"),
            Set.of());

    @Override
    public RuntimeBridgeModule descriptor() {
        return DESCRIPTOR;
    }

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
