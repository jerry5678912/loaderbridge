package net.fabricmc.fabric.impl.attachment.sync;

import java.util.function.Consumer;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.impl.attachment.sync.s2c.RequestAcceptedAttachmentsPayloadS2C;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ConfigurationTask;

public final class AttachmentSyncTask implements ConfigurationTask {
    public static final Type TYPE = new Type(
            "loaderbridge:fabric_accepted_attachments_v1");

    @Override public void start(Consumer<Packet<?>> sender) {
        sender.accept(ServerConfigurationNetworking.createS2CPacket(
                RequestAcceptedAttachmentsPayloadS2C.INSTANCE));
    }

    @Override public Type type() { return TYPE; }
}
