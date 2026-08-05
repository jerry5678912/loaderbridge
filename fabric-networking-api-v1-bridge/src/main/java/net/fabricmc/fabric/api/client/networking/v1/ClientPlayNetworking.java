package net.fabricmc.fabric.api.client.networking.v1;

import dev.loaderbridge.fabric.api.networking.NetworkBridgeRuntime;
import java.util.Objects;
import java.util.Set;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraftforge.network.NetworkProtocol;

public final class ClientPlayNetworking {
    private static final Map<ClientPacketListener, Map<ResourceLocation, PlayPayloadHandler<?>>> LOCAL =
            Collections.synchronizedMap(new IdentityHashMap<>());
    public static <T extends CustomPacketPayload> boolean registerGlobalReceiver(
            CustomPacketPayload.Type<T> type, PlayPayloadHandler<T> handler) {
        return NetworkBridgeRuntime.registerClientPlayReceiver(type, handler);
    }

    @Nullable public static PlayPayloadHandler<?> unregisterGlobalReceiver(ResourceLocation id) {
        return NetworkBridgeRuntime.unregisterClientPlayReceiver(id);
    }

    public static Set<ResourceLocation> getGlobalReceivers() {
        return NetworkBridgeRuntime.clientPlayReceivers();
    }

    public static <T extends CustomPacketPayload> boolean registerReceiver(
            CustomPacketPayload.Type<T> type, PlayPayloadHandler<T> handler) {
        ClientPacketListener listener = requireConnection();
        Map<ResourceLocation, PlayPayloadHandler<?>> receivers = LOCAL.computeIfAbsent(
                listener, ignored -> new ConcurrentHashMap<>());
        return receivers.putIfAbsent(type.id(), handler) == null;
    }

    @Nullable public static PlayPayloadHandler<?> unregisterReceiver(ResourceLocation id) {
        Map<ResourceLocation, PlayPayloadHandler<?>> receivers = LOCAL.get(requireConnection());
        return receivers == null ? null : receivers.remove(id);
    }

    public static Set<ResourceLocation> getReceived() {
        Map<ResourceLocation, PlayPayloadHandler<?>> local = LOCAL.get(requireConnection());
        if (local == null || local.isEmpty()) return getGlobalReceivers();
        var result = new java.util.HashSet<>(getGlobalReceivers());
        result.addAll(local.keySet());
        return Set.copyOf(result);
    }

    public static Set<ResourceLocation> getSendable() {
        ClientPacketListener listener = requireConnection();
        return NetworkBridgeRuntime.remotePlayC2SChannels(listener.getConnection());
    }

    public static boolean canSend(ResourceLocation channelName) {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        return listener != null && NetworkBridgeRuntime.remotePlayC2SChannels(
                listener.getConnection()).contains(channelName);
    }

    public static boolean canSend(CustomPacketPayload.Type<?> type) { return canSend(type.id()); }

    public static <T extends CustomPacketPayload> Packet<ServerCommonPacketListener> createC2SPacket(T payload) {
        return NetworkProtocol.PLAY.buildPacket(PacketFlow.SERVERBOUND,
                NetworkBridgeRuntime.channel(), NetworkBridgeRuntime.outboundPlayC2S(payload));
    }

    public static PacketSender getSender() { return new ClientPacketSender(requireConnection()); }

    public static void send(CustomPacketPayload payload) {
        Objects.requireNonNull(payload, "Payload cannot be null");
        ClientPacketListener listener = requireConnection();
        NetworkBridgeRuntime.channel().send(NetworkBridgeRuntime.outboundPlayC2S(payload),
                listener.getConnection());
    }

    public static Context context() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = Objects.requireNonNull(client.player, "Client player cannot be null");
        return new Context() {
            @Override public Minecraft client() { return client; }
            @Override public LocalPlayer player() { return player; }
            @Override public PacketSender responseSender() { return getSender(); }
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void dispatch(CustomPacketPayload payload) {
        ClientPacketListener listener = requireConnection();
        Map<ResourceLocation, PlayPayloadHandler<?>> local = LOCAL.get(listener);
        PlayPayloadHandler handler = local == null ? null : local.get(payload.type().id());
        if (handler == null) {
            handler = NetworkBridgeRuntime.clientPlayReceiver(payload.type().id());
        }
        if (handler != null) handler.receive(payload, context());
    }

    public static void clearConnection(ClientPacketListener listener) { LOCAL.remove(listener); }
    public static void clearLocalReceivers() { LOCAL.clear(); }

    private static ClientPacketListener requireConnection() {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        if (listener == null) throw new IllegalStateException("Cannot use play networking while not in game!");
        return listener;
    }

    @FunctionalInterface public interface PlayPayloadHandler<T extends CustomPacketPayload> {
        void receive(T payload, Context context);
    }

    public interface Context {
        Minecraft client();
        LocalPlayer player();
        PacketSender responseSender();
    }

    private record ClientPacketSender(ClientPacketListener listener) implements PacketSender {
        @Override public Packet<?> createPacket(CustomPacketPayload payload) { return createC2SPacket(payload); }
        @Override public void sendPacket(Packet<?> packet, @Nullable PacketSendListener callback) {
            listener.getConnection().send(packet, callback);
        }
        @Override public void disconnect(Component reason) { listener.getConnection().disconnect(reason); }
    }

    private ClientPlayNetworking() { }
}
