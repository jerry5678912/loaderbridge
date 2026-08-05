package net.fabricmc.fabric.impl.attachment.sync;

import dev.loaderbridge.fabric.api.networking.NetworkBridgeRuntime;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.impl.attachment.AttachmentRegistryImpl;
import net.fabricmc.fabric.impl.attachment.sync.c2s.AcceptedAttachmentsPayloadC2S;
import net.fabricmc.fabric.impl.attachment.sync.s2c.RequestAcceptedAttachmentsPayloadS2C;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.GatherLoginConfigurationTasksEvent;

public final class AttachmentNegotiation {
    private static final System.Logger LOGGER = System.getLogger(
            AttachmentNegotiation.class.getName());

    public static void initialize() {
        PayloadTypeRegistry.configurationC2S().register(
                AcceptedAttachmentsPayloadC2S.ID, AcceptedAttachmentsPayloadC2S.CODEC);
        PayloadTypeRegistry.configurationS2C().register(
                RequestAcceptedAttachmentsPayloadS2C.ID,
                RequestAcceptedAttachmentsPayloadS2C.CODEC);
        ServerConfigurationNetworking.registerGlobalReceiver(
                AcceptedAttachmentsPayloadC2S.ID, (payload, context) -> {
                    Set<ResourceLocation> supported = intersectSupported(
                            payload.acceptedAttachments());
                    Connection connection = context.networkHandler().getConnection();
                    ((SupportedAttachmentsConnection) connection)
                            .loaderbridge$setSupportedAttachments(supported);
                    LOGGER.log(System.Logger.Level.INFO,
                            "LoaderBridge negotiated {0} Fabric attachment types",
                            supported.size());
                    context.networkHandler().finishCurrentTask(AttachmentSyncTask.TYPE);
                });
    }

    public static void initializeClient() {
        ClientConfigurationNetworking.registerGlobalReceiver(
                RequestAcceptedAttachmentsPayloadS2C.ID, (payload, context) ->
                        context.responseSender().sendPacket(new AcceptedAttachmentsPayloadC2S(
                                AttachmentRegistryImpl.getSyncableAttachments())));
    }

    public static void gatherLoginTask(GatherLoginConfigurationTasksEvent event) {
        Connection connection = event.getConnection();
        boolean supported = NetworkBridgeRuntime.remoteChannels(connection,
                NetworkBridgeRuntime.configurationS2CChannels())
                .contains(RequestAcceptedAttachmentsPayloadS2C.PACKET_ID);
        if (supported) {
            event.addTask(new AttachmentSyncTask());
        } else {
            ((SupportedAttachmentsConnection) connection)
                    .loaderbridge$setSupportedAttachments(Set.of());
            LOGGER.log(System.Logger.Level.INFO,
                    "Client does not support Fabric attachment negotiation");
        }
    }

    public static boolean supports(ServerPlayer player, AttachmentType<?> type) {
        return ((SupportedAttachmentsConnection) player.connection.getConnection())
                .loaderbridge$getSupportedAttachments().contains(type.identifier());
    }

    static Set<ResourceLocation> intersectSupported(Set<ResourceLocation> clientTypes) {
        Set<ResourceLocation> result = new HashSet<>(clientTypes);
        result.retainAll(AttachmentRegistryImpl.getSyncableAttachments());
        return Set.copyOf(result);
    }

    private AttachmentNegotiation() { }
}
