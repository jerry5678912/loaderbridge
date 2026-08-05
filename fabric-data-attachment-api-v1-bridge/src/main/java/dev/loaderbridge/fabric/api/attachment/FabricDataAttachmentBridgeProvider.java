package dev.loaderbridge.fabric.api.attachment;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class FabricDataAttachmentBridgeProvider implements RuntimeBridgeModuleProvider {
    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            "fabric-data-attachment-api-v1-bridge",
            "fabric-data-attachment-api-v1:1.4.7",
            "1.4.7+5b36e0f719-loaderbridge.1",
            BridgeCapability.FABRIC_API,
            Set.of(
                    "net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry",
                    "net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry$Builder",
                    "net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate",
                    "net.fabricmc.fabric.api.attachment.v1.AttachmentTarget",
                    "net.fabricmc.fabric.api.attachment.v1.AttachmentType"),
            Map.of("fabric-data-attachment-api-v1", "1.4.7+5b36e0f719"),
            Set.of("fabric-entity-events-v1-bridge", "fabric-object-builder-api-v1-bridge",
                    "fabric-networking-api-v1-bridge"));

    @Override public RuntimeBridgeModule descriptor() { return DESCRIPTOR; }

    @Override public Path artifact() throws IOException {
        try {
            Path path = Path.of(getClass().getProtectionDomain().getCodeSource()
                    .getLocation().toURI());
            if (!Files.isRegularFile(path) || !path.getFileName().toString().endsWith(".jar")) {
                throw new IOException("LB-MODULE-002: bridge module is not running from a JAR: "
                        + path);
            }
            return path;
        } catch (URISyntaxException exception) {
            throw new IOException("LB-MODULE-002: invalid bridge module location", exception);
        }
    }
}
