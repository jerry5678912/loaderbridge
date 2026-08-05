package net.fabricmc.fabric.impl.attachment.sync.c2s;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AcceptedAttachmentsPayloadC2S(Set<ResourceLocation> acceptedAttachments)
        implements CustomPacketPayload {
    public static final ResourceLocation PACKET_ID = ResourceLocation.fromNamespaceAndPath(
            "loaderbridge", "fabric_accepted_attachments_response_v1");
    public static final Type<AcceptedAttachmentsPayloadC2S> ID = new Type<>(PACKET_ID);
    public static final StreamCodec<FriendlyByteBuf, AcceptedAttachmentsPayloadC2S> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.collection(HashSet::new, ResourceLocation.STREAM_CODEC),
                    AcceptedAttachmentsPayloadC2S::acceptedAttachments,
                    AcceptedAttachmentsPayloadC2S::new);

    public AcceptedAttachmentsPayloadC2S {
        acceptedAttachments = Set.copyOf(acceptedAttachments);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return ID; }
}
