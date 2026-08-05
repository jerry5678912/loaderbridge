package net.fabricmc.fabric.impl.attachment.sync.s2c;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.impl.attachment.sync.AttachmentChange;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AttachmentSyncPayloadS2C(List<AttachmentChange> attachments)
        implements CustomPacketPayload {
    private static final int MAX_CHANGES = 4096;
    public static final Type<AttachmentSyncPayloadS2C> ID = new Type<>(
            ResourceLocation.fromNamespaceAndPath("fabric", "attachment_sync_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AttachmentSyncPayloadS2C> CODEC =
            StreamCodec.ofMember(AttachmentSyncPayloadS2C::encode, AttachmentSyncPayloadS2C::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(attachments.size());
        attachments.forEach(change -> change.encode(buffer));
    }

    private static AttachmentSyncPayloadS2C decode(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_CHANGES) {
            throw new IllegalArgumentException("LB-ATTACH-004: invalid attachment batch " + size);
        }
        List<AttachmentChange> changes = new ArrayList<>(size);
        for (int index = 0; index < size; index++) changes.add(AttachmentChange.decode(buffer));
        return new AttachmentSyncPayloadS2C(List.copyOf(changes));
    }

    @Override public Type<? extends CustomPacketPayload> type() { return ID; }
}
