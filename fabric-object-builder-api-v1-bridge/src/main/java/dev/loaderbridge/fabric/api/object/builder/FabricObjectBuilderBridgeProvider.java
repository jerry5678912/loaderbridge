package dev.loaderbridge.fabric.api.object.builder;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/** Advertises the object-builder contracts currently implemented by this bridge. */
public final class FabricObjectBuilderBridgeProvider implements RuntimeBridgeModuleProvider {
    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            "fabric-object-builder-api-v1-bridge",
            "fabric-object-builder-api-v1:15.2.1",
            "15.2.1+40875a9319-loaderbridge.5",
            BridgeCapability.FABRIC_API,
            Set.of(
                    "net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder",
                    "net.fabricmc.fabric.api.object.builder.v1.block.entity."
                            + "FabricBlockEntityTypeBuilder$Factory",
                    "net.fabricmc.fabric.api.object.builder.v1.entity."
                            + "FabricDefaultAttributeRegistry",
                    "net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder",
                    "net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder$Living",
                    "net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder$Mob",
                    "net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType",
                    "net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType$Builder",
                    "net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType$Builder$Living",
                    "net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType$Builder$Mob",
                    "net.fabricmc.fabric.api.object.builder.v1.block.type.BlockSetTypeBuilder",
                    "net.fabricmc.fabric.api.object.builder.v1.block.type.WoodTypeBuilder"),
            Map.of("fabric-object-builder-api-v1", "15.2.1+40875a9319"),
            Set.of());

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
