package dev.loaderbridge.fixture.lifecycle;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

record FabricNetworkingPayload(Type<FabricNetworkingPayload> packetType, String value)
        implements CustomPacketPayload {
    static final Type<FabricNetworkingPayload> PLAY_TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("loaderbridge", "bidirectional_play"));
    static final Type<FabricNetworkingPayload> CONFIG_TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("loaderbridge", "bidirectional_config"));
    static final StreamCodec<RegistryFriendlyByteBuf, FabricNetworkingPayload> PLAY_CODEC =
            codec(PLAY_TYPE);
    static final StreamCodec<FriendlyByteBuf, FabricNetworkingPayload> CONFIG_CODEC =
            configurationCodec(CONFIG_TYPE);

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
