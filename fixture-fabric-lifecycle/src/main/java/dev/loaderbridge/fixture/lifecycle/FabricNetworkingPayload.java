package dev.loaderbridge.fixture.lifecycle;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

record FabricNetworkingPayload(Type<FabricNetworkingPayload> packetType, String value)
        implements CustomPacketPayload {
    static final Type<FabricNetworkingPayload> PING_TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("loaderbridge", "ping"));
    static final Type<FabricNetworkingPayload> PONG_TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("loaderbridge", "pong"));
    static final Type<FabricNetworkingPayload> CONFIG_PING_TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("loaderbridge", "config_ping"));
    static final Type<FabricNetworkingPayload> CONFIG_PONG_TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("loaderbridge", "config_pong"));
    static final StreamCodec<RegistryFriendlyByteBuf, FabricNetworkingPayload> PING_CODEC = codec(PING_TYPE);
    static final StreamCodec<RegistryFriendlyByteBuf, FabricNetworkingPayload> PONG_CODEC = codec(PONG_TYPE);
    static final StreamCodec<FriendlyByteBuf, FabricNetworkingPayload> CONFIG_PING_CODEC =
            configurationCodec(CONFIG_PING_TYPE);
    static final StreamCodec<FriendlyByteBuf, FabricNetworkingPayload> CONFIG_PONG_CODEC =
            configurationCodec(CONFIG_PONG_TYPE);

    private static StreamCodec<RegistryFriendlyByteBuf, FabricNetworkingPayload> codec(
            Type<FabricNetworkingPayload> type) {
        return StreamCodec.of(
            (buffer, payload) -> buffer.writeUtf(payload.value),
            buffer -> new FabricNetworkingPayload(type, buffer.readUtf()));
    }

    private static StreamCodec<FriendlyByteBuf, FabricNetworkingPayload> configurationCodec(
            Type<FabricNetworkingPayload> type) {
        return StreamCodec.of(
            (buffer, payload) -> buffer.writeUtf(payload.value),
            buffer -> new FabricNetworkingPayload(type, buffer.readUtf()));
    }

    @Override public Type<? extends CustomPacketPayload> type() { return packetType; }
}
