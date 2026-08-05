package dev.loaderbridge.fabric.api.tag.convention;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class FabricConventionTagsBridgeProvider implements RuntimeBridgeModuleProvider {
    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            "fabric-convention-tags-v2-bridge",
            "fabric-convention-tags-v2:2.12.0",
            "2.12.0+c3656daa19-loaderbridge.2",
            BridgeCapability.FABRIC_API,
            Set.of("net.fabricmc.fabric.api.tag.FabricTagKey",
                    "net.fabricmc.fabric.api.tag.convention.v2.TagUtil",
                    "net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags",
                    "net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags",
                    "net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags",
                    "net.fabricmc.fabric.api.tag.convention.v2.ConventionalFluidTags",
                    "net.fabricmc.fabric.api.tag.convention.v2.ConventionalEntityTypeTags",
                    "net.fabricmc.fabric.api.tag.convention.v2.ConventionalEnchantmentTags",
                    "net.fabricmc.fabric.api.tag.convention.v2.ConventionalStructureTags"),
            Map.of("fabric-convention-tags-v2", "2.12.0+c3656daa19"),
            Set.of("fabric-lifecycle-events-bridge"));

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
