package dev.loaderbridge.fabric.api.item;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class FabricItemApiBridgeProvider implements RuntimeBridgeModuleProvider {
    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            "fabric-item-api-v1-bridge", "fabric-item-api-v1:11.3.0",
            "11.3.0+467044f319-loaderbridge.5", BridgeCapability.FABRIC_API,
            Set.of(
                    "net.fabricmc.fabric.api.item.v1.CustomDamageHandler",
                    "net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents",
                    "net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents$ModifyCallback",
                    "net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents$ModifyContext",
                    "net.fabricmc.fabric.api.item.v1.EnchantingContext",
                    "net.fabricmc.fabric.api.item.v1.EnchantmentEvents",
                    "net.fabricmc.fabric.api.item.v1.EnchantmentEvents$AllowEnchanting",
                    "net.fabricmc.fabric.api.item.v1.EnchantmentEvents$Modify",
                    "net.fabricmc.fabric.api.item.v1.EnchantmentSource",
                    "net.fabricmc.fabric.api.item.v1.EquipmentSlotProvider",
                    "net.fabricmc.fabric.api.item.v1.FabricComponentMapBuilder",
                    "net.fabricmc.fabric.api.item.v1.FabricItem",
                    "net.fabricmc.fabric.api.item.v1.FabricItem$Settings",
                    "net.fabricmc.fabric.api.item.v1.FabricItemStack",
                    "net.fabricmc.fabric.api.item.v1.FabricTooltipType"),
            Map.of("fabric-item-api-v1", "11.3.0+467044f319"),
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
