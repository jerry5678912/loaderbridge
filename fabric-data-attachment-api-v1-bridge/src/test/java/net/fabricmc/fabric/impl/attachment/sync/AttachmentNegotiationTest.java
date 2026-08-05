package net.fabricmc.fabric.impl.attachment.sync;

import static org.assertj.core.api.Assertions.assertThat;

import io.netty.buffer.Unpooled;
import java.util.Set;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.impl.attachment.sync.c2s.AcceptedAttachmentsPayloadC2S;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class AttachmentNegotiationTest {
    @Test
    void responseCodecAndServerIntersectionKeepOnlyMutuallyRegisteredTypes() {
        ResourceLocation shared = id("negotiated_shared");
        ResourceLocation clientOnly = id("negotiated_client_only");
        AttachmentRegistry.<Integer>create(shared, builder -> builder.syncWith(
                ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.all()));
        var response = new AcceptedAttachmentsPayloadC2S(Set.of(shared, clientOnly));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());

        AcceptedAttachmentsPayloadC2S.CODEC.encode(buffer, response);
        AcceptedAttachmentsPayloadC2S restored =
                AcceptedAttachmentsPayloadC2S.CODEC.decode(buffer);

        assertThat(restored.acceptedAttachments()).containsExactlyInAnyOrder(
                shared, clientOnly);
        assertThat(AttachmentNegotiation.intersectSupported(
                restored.acceptedAttachments())).containsExactly(shared);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("loaderbridge", path);
    }
}
