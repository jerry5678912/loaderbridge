package net.fabricmc.fabric.api.networking.v1;

import dev.loaderbridge.fabric.api.networking.NetworkBridgeRuntime;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraftforge.network.NetworkProtocol;
import org.jetbrains.annotations.Nullable;

public final class ServerPlayNetworking {
    private static final Map<ServerGamePacketListenerImpl,
            Map<ResourceLocation, PlayPayloadHandler<?>>> LOCAL =
            Collections.synchronizedMap(new IdentityHashMap<>());

    public static <T extends CustomPacketPayload> boolean registerGlobalReceiver(
            CustomPacketPayload.Type<T> type, PlayPayloadHandler<T> handler) {
        return NetworkBridgeRuntime.registerPlayReceiver(type, handler);
    }

    @Nullable public static PlayPayloadHandler<?> unregisterGlobalReceiver(ResourceLocation id) {
        return NetworkBridgeRuntime.unregisterPlayReceiver(Objects.requireNonNull(id, "Payload id cannot be null"));
    }

    public static Set<ResourceLocation> getGlobalReceivers() { return NetworkBridgeRuntime.playReceivers(); }

    public static <T extends CustomPacketPayload> boolean registerReceiver(
            ServerGamePacketListenerImpl networkHandler, CustomPacketPayload.Type<T> type,
            PlayPayloadHandler<T> handler) {
        Objects.requireNonNull(networkHandler, "Server play network handler cannot be null");
        Objects.requireNonNull(type, "Packet type cannot be null");
        Objects.requireNonNull(handler, "Packet handler cannot be null");
        Map<ResourceLocation, PlayPayloadHandler<?>> receivers = LOCAL.computeIfAbsent(
                networkHandler, ignored -> new ConcurrentHashMap<>());
        return receivers.putIfAbsent(type.id(), handler) == null;
    }

    @Nullable public static PlayPayloadHandler<?> unregisterReceiver(
            ServerGamePacketListenerImpl networkHandler, ResourceLocation id) {
        Objects.requireNonNull(networkHandler, "Server play network handler cannot be null");
        Map<ResourceLocation, PlayPayloadHandler<?>> receivers = LOCAL.get(networkHandler);
        return receivers == null ? null : receivers.remove(id);
    }

    public static Set<ResourceLocation> getReceived(ServerPlayer player) { return getReceived(player.connection); }

    public static Set<ResourceLocation> getReceived(ServerGamePacketListenerImpl handler) {
        Objects.requireNonNull(handler, "Server play network handler cannot be null");
        Map<ResourceLocation, PlayPayloadHandler<?>> local = LOCAL.get(handler);
        if (local == null || local.isEmpty()) return getGlobalReceivers();
        var result = new java.util.HashSet<>(getGlobalReceivers());
        result.addAll(local.keySet());
        return Set.copyOf(result);
    }

    public static Set<ResourceLocation> getSendable(ServerPlayer player) { return getSendable(player.connection); }

    public static Set<ResourceLocation> getSendable(ServerGamePacketListenerImpl handler) {
        Objects.requireNonNull(handler, "Server play network handler cannot be null");
        return NetworkBridgeRuntime.remoteChannels(handler.getConnection(),
                NetworkBridgeRuntime.playS2CChannels());
    }

    public static boolean canSend(ServerPlayer player, ResourceLocation channelName) {
        return canSend(player.connection, channelName);
    }

    public static boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return canSend(player.connection, type.id());
    }

    public static boolean canSend(ServerGamePacketListenerImpl handler, ResourceLocation channelName) {
        Objects.requireNonNull(channelName, "Channel name cannot be null");
        return getSendable(handler).contains(channelName);
    }

    public static boolean canSend(ServerGamePacketListenerImpl handler, CustomPacketPayload.Type<?> type) {
        Objects.requireNonNull(type, "Packet type cannot be null");
        return canSend(handler, type.id());
    }

    public static <T extends CustomPacketPayload> Packet<ClientCommonPacketListener> createS2CPacket(T payload) {
        Objects.requireNonNull(payload, "Payload cannot be null");
        return NetworkProtocol.PLAY.buildPacket(PacketFlow.CLIENTBOUND,
                NetworkBridgeRuntime.channel(), payload);
    }

    public static PacketSender getSender(ServerPlayer player) { return getSender(player.connection); }

    public static PacketSender getSender(ServerGamePacketListenerImpl handler) {
        Objects.requireNonNull(handler, "Server play network handler cannot be null");
        return new HandlerPacketSender(handler);
    }

    public static void send(ServerPlayer player, CustomPacketPayload payload) {
        Objects.requireNonNull(player, "Server player entity cannot be null");
        Objects.requireNonNull(payload, "Payload cannot be null");
        NetworkBridgeRuntime.channel().send(payload, player.connection.getConnection());
    }

    public static Context context(ServerPlayer player) {
        return new Context() {
            @Override public MinecraftServer server() { return player.server; }
            @Override public ServerPlayer player() { return player; }
            @Override public PacketSender responseSender() { return getSender(player); }
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void dispatch(CustomPacketPayload payload, ServerPlayer player) {
        Map<ResourceLocation, PlayPayloadHandler<?>> local = LOCAL.get(player.connection);
        PlayPayloadHandler handler = local == null ? null : local.get(payload.type().id());
        if (handler == null) handler = NetworkBridgeRuntime.globalPlayReceiver(payload.type().id());
        if (handler != null) handler.receive(payload, context(player));
    }

    public static void clear(ServerGamePacketListenerImpl handler) { LOCAL.remove(handler); }

    @FunctionalInterface
    public interface PlayPayloadHandler<T extends CustomPacketPayload> {
        void receive(T payload, Context context);
    }

    public interface Context {
        MinecraftServer server();
        ServerPlayer player();
        PacketSender responseSender();
    }

    private record HandlerPacketSender(ServerGamePacketListenerImpl handler) implements PacketSender {
        @Override public Packet<?> createPacket(CustomPacketPayload payload) {
            return createS2CPacket(payload);
        }
        @Override public void sendPacket(Packet<?> packet, @Nullable PacketSendListener callback) {
            handler.send(packet, callback);
        }
        @Override public void disconnect(Component reason) { handler.disconnect(reason); }
    }

    private ServerPlayNetworking() { }
}
