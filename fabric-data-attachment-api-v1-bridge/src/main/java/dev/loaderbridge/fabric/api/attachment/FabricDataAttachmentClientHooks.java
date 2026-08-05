package dev.loaderbridge.fabric.api.attachment;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.impl.attachment.sync.s2c.AttachmentSyncPayloadS2C;
import net.fabricmc.fabric.impl.attachment.sync.AttachmentNegotiation;

/** Client-only synchronization linkage kept out of common method signatures. */
final class FabricDataAttachmentClientHooks {
    static void register() {
        AttachmentNegotiation.initializeClient();
        ClientPlayNetworking.registerGlobalReceiver(AttachmentSyncPayloadS2C.ID,
                (payload, context) -> {
                    if (context.client().level == null) return;
                    payload.attachments().forEach(change -> change.apply(context.client().level));
                });
    }

    private FabricDataAttachmentClientHooks() { }
}
