package dev.loaderbridge.fabric.api.networking;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.payload.PayloadConnection;

public final class NetworkBridgeRuntime {
    private static final Registry<FriendlyByteBuf> CONFIG_C2S = new Registry<>("configuration C2S");
    private static final Registry<FriendlyByteBuf> CONFIG_S2C = new Registry<>("configuration S2C");
    private static final Registry<RegistryFriendlyByteBuf> PLAY_C2S = new Registry<>("play C2S");
    private static final Registry<RegistryFriendlyByteBuf> PLAY_S2C = new Registry<>("play S2C");
    private static final Map<ResourceLocation, ServerPlayNetworking.PlayPayloadHandler<?>> PLAY_GLOBAL =
            new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, ServerConfigurationNetworking.ConfigurationPacketHandler<?>>
            CONFIG_SERVER_GLOBAL = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, ClientPlayNetworking.PlayPayloadHandler<?>> CLIENT_PLAY_GLOBAL =
            new ConcurrentHashMap<>();
    private static volatile Channel<CustomPacketPayload> channel;

    public static PayloadTypeRegistry<FriendlyByteBuf> configurationC2S() { return CONFIG_C2S; }
    public static PayloadTypeRegistry<FriendlyByteBuf> configurationS2C() { return CONFIG_S2C; }
    public static PayloadTypeRegistry<RegistryFriendlyByteBuf> playC2S() { return PLAY_C2S; }
    public static PayloadTypeRegistry<RegistryFriendlyByteBuf> playS2C() { return PLAY_S2C; }

    public static boolean registerPlayReceiver(CustomPacketPayload.Type<?> type,
            ServerPlayNetworking.PlayPayloadHandler<?> handler) {
        Objects.requireNonNull(type, "Packet type cannot be null");
        Objects.requireNonNull(handler, "Packet handler cannot be null");
        if (!PLAY_C2S.contains(type.id())) {
            throw new IllegalArgumentException("LB-NET-003: no play C2S codec registered for " + type.id());
        }
        return PLAY_GLOBAL.putIfAbsent(type.id(), handler) == null;
    }

    public static ServerPlayNetworking.PlayPayloadHandler<?> unregisterPlayReceiver(ResourceLocation id) {
        return PLAY_GLOBAL.remove(id);
    }

    public static ServerPlayNetworking.PlayPayloadHandler<?> globalPlayReceiver(ResourceLocation id) {
        return PLAY_GLOBAL.get(id);
    }

    public static Set<ResourceLocation> playReceivers() {
        return Collections.unmodifiableSet(PLAY_GLOBAL.keySet());
    }

    public static boolean registerClientPlayReceiver(CustomPacketPayload.Type<?> type,
            ClientPlayNetworking.PlayPayloadHandler<?> handler) {
        Objects.requireNonNull(type, "Packet type cannot be null");
        Objects.requireNonNull(handler, "Packet handler cannot be null");
        if (!PLAY_S2C.contains(type.id())) {
            throw new IllegalArgumentException("LB-NET-003: no play S2C codec registered for " + type.id());
        }
        return CLIENT_PLAY_GLOBAL.putIfAbsent(type.id(), handler) == null;
    }

    public static ClientPlayNetworking.PlayPayloadHandler<?> unregisterClientPlayReceiver(ResourceLocation id) {
        return CLIENT_PLAY_GLOBAL.remove(id);
    }

    public static ClientPlayNetworking.PlayPayloadHandler<?> clientPlayReceiver(ResourceLocation id) {
        return CLIENT_PLAY_GLOBAL.get(id);
    }

    public static Set<ResourceLocation> clientPlayReceivers() {
        return Collections.unmodifiableSet(CLIENT_PLAY_GLOBAL.keySet());
    }

    public static Set<ResourceLocation> playC2SChannels() { return PLAY_C2S.ids(); }
    public static Set<ResourceLocation> playS2CChannels() { return PLAY_S2C.ids(); }
    public static Set<ResourceLocation> configurationC2SChannels() { return CONFIG_C2S.ids(); }
    public static Set<ResourceLocation> configurationS2CChannels() { return CONFIG_S2C.ids(); }

    public static boolean registerServerConfigurationReceiver(CustomPacketPayload.Type<?> type,
            ServerConfigurationNetworking.ConfigurationPacketHandler<?> handler) {
        Objects.requireNonNull(type, "Packet type cannot be null");
        Objects.requireNonNull(handler, "Packet handler cannot be null");
        if (!CONFIG_C2S.contains(type.id())) {
            throw new IllegalArgumentException(
                    "LB-NET-003: no configuration C2S codec registered for " + type.id());
        }
        return CONFIG_SERVER_GLOBAL.putIfAbsent(type.id(), handler) == null;
    }

    public static ServerConfigurationNetworking.ConfigurationPacketHandler<?>
            unregisterServerConfigurationReceiver(ResourceLocation id) {
        return CONFIG_SERVER_GLOBAL.remove(id);
    }

    public static ServerConfigurationNetworking.ConfigurationPacketHandler<?>
            serverConfigurationReceiver(ResourceLocation id) {
        return CONFIG_SERVER_GLOBAL.get(id);
    }

    public static Set<ResourceLocation> serverConfigurationReceivers() {
        return Collections.unmodifiableSet(CONFIG_SERVER_GLOBAL.keySet());
    }

    public static synchronized void finalizeRegistrations() {
        if (channel != null) return;
        PayloadConnection<CustomPacketPayload> builder = ChannelBuilder
                .named(ResourceLocation.fromNamespaceAndPath("loaderbridge", "fabric_networking"))
                .optional().payloadChannel();
        var buildable = builder.play().flow(PacketFlow.SERVERBOUND);
        PLAY_C2S.addTo(buildable, true, NetworkBridgeRuntime::dispatchPlay);
        PLAY_S2C.addTo(builder.play().flow(PacketFlow.CLIENTBOUND), true,
                NetworkBridgeRuntime::dispatchPlay);
        CONFIG_C2S.addTo(builder.configuration().flow(PacketFlow.SERVERBOUND), false,
                NetworkBridgeRuntime::dispatchServerConfiguration);
        CONFIG_S2C.addTo(builder.configuration().flow(PacketFlow.CLIENTBOUND), false, null);
        channel = buildable.build();
        CONFIG_C2S.freeze();
        CONFIG_S2C.freeze();
        PLAY_C2S.freeze();
        PLAY_S2C.freeze();
    }

    public static Channel<CustomPacketPayload> channel() {
        Channel<CustomPacketPayload> result = channel;
        if (result == null) throw new IllegalStateException("LB-NET-004: networking channel is not finalized");
        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void dispatchPlay(CustomPacketPayload payload,
            net.minecraftforge.event.network.CustomPayloadEvent.Context forgeContext) {
        if (forgeContext.isClientSide()) {
            ClientPlayNetworking.dispatch(payload);
        } else {
            var player = forgeContext.getSender();
            if (player == null) throw new IllegalStateException("LB-NET-005: serverbound play payload has no player");
            ServerPlayNetworking.dispatch(payload, player);
        }
    }

    private static void dispatchServerConfiguration(CustomPacketPayload payload,
            net.minecraftforge.event.network.CustomPayloadEvent.Context forgeContext) {
        var listener = forgeContext.getConnection().getPacketListener();
        if (!(listener instanceof net.minecraft.server.network.ServerConfigurationPacketListenerImpl handler)) {
            throw new IllegalStateException(
                    "LB-NET-006: serverbound configuration payload has no configuration listener");
        }
        ServerConfigurationNetworking.dispatch(payload, handler);
    }

    @FunctionalInterface
    private interface PayloadDispatcher {
        void dispatch(CustomPacketPayload payload,
                net.minecraftforge.event.network.CustomPayloadEvent.Context context);
    }

    private static final class Registry<B extends FriendlyByteBuf> implements PayloadTypeRegistry<B> {
        private final String phase;
        private final Map<ResourceLocation, CustomPacketPayload.TypeAndCodec<? super B, ?>> entries =
                new LinkedHashMap<>();
        private boolean frozen;

        private Registry(String phase) { this.phase = phase; }

        @Override @SuppressWarnings("unchecked") public synchronized <T extends CustomPacketPayload>
                CustomPacketPayload.TypeAndCodec<? super B, T> register(
                        CustomPacketPayload.Type<T> id, StreamCodec<? super B, T> codec) {
            Objects.requireNonNull(id, "Payload type cannot be null");
            Objects.requireNonNull(codec, "Payload codec cannot be null");
            if (frozen) throw new IllegalStateException("LB-NET-001: late payload registration in " + phase);
            var entry = new CustomPacketPayload.TypeAndCodec<B, T>(id, (StreamCodec<B, T>) codec);
            if (entries.putIfAbsent(id.id(), entry) != null) {
                throw new IllegalArgumentException("LB-NET-002: duplicate payload type " + id.id());
            }
            return entry;
        }

        synchronized boolean contains(ResourceLocation id) { return entries.containsKey(id); }
        synchronized Set<ResourceLocation> ids() { return Set.copyOf(entries.keySet()); }
        synchronized void freeze() { frozen = true; }

        @SuppressWarnings("unchecked")
        synchronized void addTo(
                net.minecraftforge.network.payload.PayloadFlow<B, CustomPacketPayload> flow,
                boolean enqueue, PayloadDispatcher dispatcher) {
            for (var value : entries.values()) {
                var entry = (CustomPacketPayload.TypeAndCodec<B, CustomPacketPayload>) value;
                flow.add(entry.type(), entry.codec(), (payload, context) -> {
                    if (dispatcher != null) {
                        if (enqueue) context.enqueueWork(() -> dispatcher.dispatch(payload, context));
                        else dispatcher.dispatch(payload, context);
                    }
                    context.setPacketHandled(true);
                });
            }
        }
    }

    private NetworkBridgeRuntime() { }
}
