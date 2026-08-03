package net.fabricmc.fabric.api.client.networking.v1;

import dev.loaderbridge.fabric.api.networking.NetworkBridgeRuntime;
import dev.loaderbridge.fabric.api.networking.mixin.ClientCommonPacketListenerAccessor;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientConfigurationPacketListenerImpl;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkProtocol;
import org.jetbrains.annotations.Nullable;

public final class ClientConfigurationNetworking {
    private static final Map<ClientConfigurationPacketListenerImpl,
            Map<ResourceLocation, ConfigurationPayloadHandler<?>>> LOCAL =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private static volatile ClientConfigurationPacketListenerImpl current;

    public static <T extends CustomPacketPayload> boolean registerGlobalReceiver(
            CustomPacketPayload.Type<T> type, ConfigurationPayloadHandler<T> handler) {
        return NetworkBridgeRuntime.registerClientConfigurationReceiver(type, handler);
    }

    @Nullable public static ConfigurationPayloadHandler<?> unregisterGlobalReceiver(
            CustomPacketPayload.Type<?> type) {
        Objects.requireNonNull(type, "Payload type cannot be null");
        return NetworkBridgeRuntime.unregisterClientConfigurationReceiver(type.id());
    }

    public static Set<ResourceLocation> getGlobalReceivers() {
        return NetworkBridgeRuntime.clientConfigurationReceivers();
    }

    public static <T extends CustomPacketPayload> boolean registerReceiver(
            CustomPacketPayload.Type<T> type, ConfigurationPayloadHandler<T> handler) {
        ClientConfigurationPacketListenerImpl listener = requireConnection();
        Objects.requireNonNull(type, "Packet type cannot be null");
        Objects.requireNonNull(handler, "Packet handler cannot be null");
        if (!NetworkBridgeRuntime.configurationS2CChannels().contains(type.id())) {
            throw new IllegalArgumentException(
                    "LB-NET-003: no configuration S2C codec registered for " + type.id());
        }
        return LOCAL.computeIfAbsent(listener, ignored -> new ConcurrentHashMap<>())
                .putIfAbsent(type.id(), handler) == null;
    }

    @Nullable public static ConfigurationPayloadHandler<?> unregisterReceiver(ResourceLocation id) {
        Map<ResourceLocation, ConfigurationPayloadHandler<?>> receivers = LOCAL.get(requireConnection());
        return receivers == null ? null : receivers.remove(id);
    }

    public static Set<ResourceLocation> getReceived() {
        Map<ResourceLocation, ConfigurationPayloadHandler<?>> local = LOCAL.get(requireConnection());
        if (local == null || local.isEmpty()) return getGlobalReceivers();
        var result = new java.util.HashSet<>(getGlobalReceivers());
        result.addAll(local.keySet());
        return Set.copyOf(result);
    }

    public static Set<ResourceLocation> getSendable() {
        ClientConfigurationPacketListenerImpl listener = requireConnection();
        return NetworkBridgeRuntime.remoteChannels(connection(listener),
                NetworkBridgeRuntime.configurationC2SChannels());
    }

    public static boolean canSend(ResourceLocation channelName) {
        Objects.requireNonNull(channelName, "Channel name cannot be null");
        return getSendable().contains(channelName);
    }

    public static boolean canSend(CustomPacketPayload.Type<?> type) {
        Objects.requireNonNull(type, "Payload type cannot be null");
        return canSend(type.id());
    }

    public static Packet<ServerCommonPacketListener> createC2SPacket(CustomPacketPayload payload) {
        Objects.requireNonNull(payload, "Payload cannot be null");
        return NetworkProtocol.CONFIGURATION.buildPacket(PacketFlow.SERVERBOUND,
                NetworkBridgeRuntime.channel(), payload);
    }

    public static PacketSender getSender() { return new ClientPacketSender(requireConnection()); }

    public static void send(CustomPacketPayload payload) {
        Objects.requireNonNull(payload, "Payload cannot be null");
        ClientConfigurationPacketListenerImpl listener = requireConnection();
        NetworkBridgeRuntime.channel().send(payload, connection(listener));
    }

    public static Context context(ClientConfigurationPacketListenerImpl listener) {
        return new Context() {
            @Override public Minecraft client() { return Minecraft.getInstance(); }
            @Override public ClientConfigurationPacketListenerImpl networkHandler() { return listener; }
            @Override public PacketSender responseSender() {
                return new ClientPacketSender(listener);
            }
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void dispatch(CustomPacketPayload payload,
            ClientConfigurationPacketListenerImpl listener) {
        Map<ResourceLocation, ConfigurationPayloadHandler<?>> local = LOCAL.get(listener);
        ConfigurationPayloadHandler receiver = local == null ? null : local.get(payload.type().id());
        if (receiver == null) {
            receiver = NetworkBridgeRuntime.clientConfigurationReceiver(payload.type().id());
        }
        if (receiver != null) receiver.receive(payload, context(listener));
    }

    public static void setCurrent(ClientConfigurationPacketListenerImpl listener) { current = listener; }

    public static void clear(ClientConfigurationPacketListenerImpl listener) {
        LOCAL.remove(listener);
        if (current == listener) current = null;
    }

    private static ClientConfigurationPacketListenerImpl requireConnection() {
        ClientConfigurationPacketListenerImpl listener = current;
        if (listener == null) {
            throw new IllegalStateException("Cannot use configuration networking while not configuring!");
        }
        return listener;
    }

    private static net.minecraft.network.Connection connection(
            ClientConfigurationPacketListenerImpl listener) {
        return ((ClientCommonPacketListenerAccessor) listener).loaderbridge$getConnection();
    }

    @FunctionalInterface public interface ConfigurationPayloadHandler<T extends CustomPacketPayload> {
        void receive(T payload, Context context);
    }

    public interface Context {
        Minecraft client();
        ClientConfigurationPacketListenerImpl networkHandler();
        PacketSender responseSender();
    }

    private record ClientPacketSender(ClientConfigurationPacketListenerImpl listener)
            implements PacketSender {
        @Override public Packet<?> createPacket(CustomPacketPayload payload) {
            return createC2SPacket(payload);
        }
        @Override public void sendPacket(Packet<?> packet, @Nullable PacketSendListener callback) {
            connection(listener).send(packet, callback);
        }
        @Override public void disconnect(Component reason) {
            connection(listener).disconnect(reason);
        }
    }

    private ClientConfigurationNetworking() { }
}
