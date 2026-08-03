package dev.loaderbridge.fixture.lifecycle;

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
    static final StreamCodec<RegistryFriendlyByteBuf, FabricNetworkingPayload> PING_CODEC = codec(PING_TYPE);
    static final StreamCodec<RegistryFriendlyByteBuf, FabricNetworkingPayload> PONG_CODEC = codec(PONG_TYPE);

    private static StreamCodec<RegistryFriendlyByteBuf, FabricNetworkingPayload> codec(
            Type<FabricNetworkingPayload> type) {
        return StreamCodec.of(
            (buffer, payload) -> buffer.writeUtf(payload.value),
            buffer -> new FabricNetworkingPayload(type, buffer.readUtf()));
    }

    @Override public Type<? extends CustomPacketPayload> type() { return packetType; }
}
