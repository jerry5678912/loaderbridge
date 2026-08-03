package net.fabricmc.fabric.api.networking.v1;

import dev.loaderbridge.fabric.api.networking.NetworkBridgeRuntime;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface PayloadTypeRegistry<B extends FriendlyByteBuf> {
    <T extends CustomPacketPayload> CustomPacketPayload.TypeAndCodec<? super B, T> register(
            CustomPacketPayload.Type<T> id, StreamCodec<? super B, T> codec);

    static PayloadTypeRegistry<FriendlyByteBuf> configurationC2S() {
        return NetworkBridgeRuntime.configurationC2S();
    }

    static PayloadTypeRegistry<FriendlyByteBuf> configurationS2C() {
        return NetworkBridgeRuntime.configurationS2C();
    }

    static PayloadTypeRegistry<RegistryFriendlyByteBuf> playC2S() {
        return NetworkBridgeRuntime.playC2S();
    }

    static PayloadTypeRegistry<RegistryFriendlyByteBuf> playS2C() {
        return NetworkBridgeRuntime.playS2C();
    }
}
