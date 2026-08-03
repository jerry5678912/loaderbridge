package dev.loaderbridge.fabric.api.transfer;

import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/** Advertises the pinned Transfer API surface implemented by the current bridge revision. */
public final class FabricTransferApiBridgeProvider implements RuntimeBridgeModuleProvider {
    private static final RuntimeBridgeModule DESCRIPTOR = new RuntimeBridgeModule(
            "fabric-transfer-api-v1-bridge",
            "fabric-transfer-api-v1:5.4.4",
            "5.4.4+7b3d111d19-loaderbridge.7",
            BridgeCapability.FABRIC_API,
            Set.of(
                    "net.fabricmc.fabric.api.transfer.v1.transaction.Transaction",
                    "net.fabricmc.fabric.api.transfer.v1.transaction.Transaction$Lifecycle",
                    "net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext",
                    "net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext$CloseCallback",
                    "net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext$OuterCloseCallback",
                    "net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext$Result",
                    "net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant",
                    "net.fabricmc.fabric.api.transfer.v1.storage.Storage",
                    "net.fabricmc.fabric.api.transfer.v1.storage.StorageView",
                    "net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant",
                    "net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions",
                    "net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil",
                    "net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage",
                    "net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount",
                    "net.fabricmc.fabric.api.transfer.v1.storage.base.BlankVariantView",
                    "net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage",
                    "net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage",
                    "net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage",
                    "net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedSlottedStorage",
                    "net.fabricmc.fabric.api.transfer.v1.storage.base.ExtractionOnlyStorage",
                    "net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage",
                    "net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage",
                    "net.fabricmc.fabric.api.transfer.v1.item.ItemVariant",
                    "net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage",
                    "net.fabricmc.fabric.api.transfer.v1.item.ItemStorage",
                    "net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage",
                    "net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext",
                    "net.fabricmc.fabric.api.transfer.v1.item.base.SingleItemStorage",
                    "net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage"),
            Map.of("fabric-transfer-api-v1", "5.4.4+7b3d111d19"),
            Set.of("fabric-api-lookup-api-v1-bridge"));

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
