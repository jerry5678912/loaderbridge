package net.fabricmc.fabric.api.networking.v1;

import dev.loaderbridge.fabric.api.networking.NetworkBridgeRuntime;
import dev.loaderbridge.fabric.api.networking.mixin.ServerCommonPacketListenerAccessor;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraftforge.network.NetworkProtocol;
import org.jetbrains.annotations.Nullable;

public final class ServerConfigurationNetworking {
    private static final Map<ServerConfigurationPacketListenerImpl,
            Map<ResourceLocation, ConfigurationPacketHandler<?>>> LOCAL =
            Collections.synchronizedMap(new IdentityHashMap<>());

    public static <T extends CustomPacketPayload> boolean registerGlobalReceiver(
            CustomPacketPayload.Type<T> type, ConfigurationPacketHandler<T> handler) {
        return NetworkBridgeRuntime.registerServerConfigurationReceiver(type, handler);
    }

    @Nullable public static ConfigurationPacketHandler<?> unregisterGlobalReceiver(ResourceLocation id) {
        return NetworkBridgeRuntime.unregisterServerConfigurationReceiver(
                Objects.requireNonNull(id, "Payload id cannot be null"));
    }

    public static Set<ResourceLocation> getGlobalReceivers() {
        return NetworkBridgeRuntime.serverConfigurationReceivers();
    }

    public static <T extends CustomPacketPayload> boolean registerReceiver(
            ServerConfigurationPacketListenerImpl networkHandler, CustomPacketPayload.Type<T> type,
            ConfigurationPacketHandler<T> handler) {
        Objects.requireNonNull(networkHandler, "Server configuration network handler cannot be null");
        Objects.requireNonNull(type, "Packet type cannot be null");
        Objects.requireNonNull(handler, "Packet handler cannot be null");
        if (!NetworkBridgeRuntime.configurationC2SChannels().contains(type.id())) {
            throw new IllegalArgumentException(
                    "LB-NET-003: no configuration C2S codec registered for " + type.id());
        }
        return LOCAL.computeIfAbsent(networkHandler, ignored -> new ConcurrentHashMap<>())
                .putIfAbsent(type.id(), handler) == null;
    }

    @Nullable public static ConfigurationPacketHandler<?> unregisterReceiver(
            ServerConfigurationPacketListenerImpl networkHandler, ResourceLocation id) {
        Objects.requireNonNull(networkHandler, "Server configuration network handler cannot be null");
        Map<ResourceLocation, ConfigurationPacketHandler<?>> receivers = LOCAL.get(networkHandler);
        return receivers == null ? null : receivers.remove(id);
    }

    public static Set<ResourceLocation> getReceived(ServerConfigurationPacketListenerImpl handler) {
        Objects.requireNonNull(handler, "Server configuration network handler cannot be null");
        Map<ResourceLocation, ConfigurationPacketHandler<?>> local = LOCAL.get(handler);
        if (local == null || local.isEmpty()) return getGlobalReceivers();
        var result = new java.util.HashSet<>(getGlobalReceivers());
        result.addAll(local.keySet());
        return Set.copyOf(result);
    }

    public static Set<ResourceLocation> getSendable(ServerConfigurationPacketListenerImpl handler) {
        Objects.requireNonNull(handler, "Server configuration network handler cannot be null");
        return NetworkBridgeRuntime.remoteConfigurationS2CChannels(handler.getConnection());
    }

    public static boolean canSend(ServerConfigurationPacketListenerImpl handler,
            ResourceLocation channelName) {
        Objects.requireNonNull(channelName, "Channel name cannot be null");
        return getSendable(handler).contains(channelName);
    }

    public static boolean canSend(ServerConfigurationPacketListenerImpl handler,
            CustomPacketPayload.Type<?> type) {
        Objects.requireNonNull(type, "Payload type cannot be null");
        return canSend(handler, type.id());
    }

    public static Packet<ClientCommonPacketListener> createS2CPacket(CustomPacketPayload payload) {
        Objects.requireNonNull(payload, "Payload cannot be null");
        return NetworkProtocol.CONFIGURATION.buildPacket(PacketFlow.CLIENTBOUND,
                NetworkBridgeRuntime.channel(),
                NetworkBridgeRuntime.outboundConfigurationS2C(payload));
    }

    public static PacketSender getSender(ServerConfigurationPacketListenerImpl handler) {
        Objects.requireNonNull(handler, "Server configuration network handler cannot be null");
        return new HandlerPacketSender(handler);
    }

    public static void send(ServerConfigurationPacketListenerImpl handler,
            CustomPacketPayload payload) {
        Objects.requireNonNull(handler, "Server configuration handler cannot be null");
        Objects.requireNonNull(payload, "Payload cannot be null");
        NetworkBridgeRuntime.channel().send(
                NetworkBridgeRuntime.outboundConfigurationS2C(payload), handler.getConnection());
    }

    public static MinecraftServer getServer(ServerConfigurationPacketListenerImpl handler) {
        Objects.requireNonNull(handler, "Network handler cannot be null");
        return ((ServerCommonPacketListenerAccessor) handler).loaderbridge$getServer();
    }

    public static Context context(ServerConfigurationPacketListenerImpl handler) {
        return new Context() {
            @Override public MinecraftServer server() { return getServer(handler); }
            @Override public ServerConfigurationPacketListenerImpl networkHandler() { return handler; }
            @Override public PacketSender responseSender() { return getSender(handler); }
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void dispatch(CustomPacketPayload payload,
            ServerConfigurationPacketListenerImpl handler) {
        Map<ResourceLocation, ConfigurationPacketHandler<?>> local = LOCAL.get(handler);
        ConfigurationPacketHandler receiver = local == null ? null : local.get(payload.type().id());
        if (receiver == null) {
            receiver = NetworkBridgeRuntime.serverConfigurationReceiver(payload.type().id());
        }
        if (receiver != null) receiver.receive(payload, context(handler));
    }

    public static void clear(ServerConfigurationPacketListenerImpl handler) { LOCAL.remove(handler); }

    @FunctionalInterface
    public interface ConfigurationPacketHandler<T extends CustomPacketPayload> {
        void receive(T payload, Context context);
    }

    public interface Context {
        MinecraftServer server();
        ServerConfigurationPacketListenerImpl networkHandler();
        PacketSender responseSender();
    }

    private record HandlerPacketSender(ServerConfigurationPacketListenerImpl handler)
            implements PacketSender {
        @Override public Packet<?> createPacket(CustomPacketPayload payload) {
            return createS2CPacket(payload);
        }
        @Override public void sendPacket(Packet<?> packet, @Nullable PacketSendListener callback) {
            handler.send(packet, callback);
        }
        @Override public void disconnect(Component reason) { handler.disconnect(reason); }
    }

    private ServerConfigurationNetworking() { }
}
