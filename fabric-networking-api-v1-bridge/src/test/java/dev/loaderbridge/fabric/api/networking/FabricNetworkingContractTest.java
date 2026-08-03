package dev.loaderbridge.fabric.api.networking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class FabricNetworkingContractTest {
    private static final StreamCodec<RegistryFriendlyByteBuf, TestPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeUtf(payload.value()),
            buffer -> new TestPayload(buffer.readUtf()));

    @Test
    void providerAdvertisesOnlyImplementedOfficialTypes() {
        var descriptor = new FabricNetworkingBridgeProvider().descriptor();
        assertThat(descriptor.contractVersion()).isEqualTo("fabric-networking-api-v1:4.3.1");
        assertThat(descriptor.implementationVersion()).isEqualTo(
                "4.3.1+d30f6a7919-loaderbridge.2");
        assertThat(descriptor.providedClasses()).containsExactlyInAnyOrder(
                "net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents",
                "net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents$StartTracking",
                "net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents$StopTracking",
                "net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking",
                "net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking$Context",
                "net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking$PlayPayloadHandler",
                "net.fabricmc.fabric.api.networking.v1.PacketByteBufs",
                "net.fabricmc.fabric.api.networking.v1.PacketSender",
                "net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry",
                "net.fabricmc.fabric.api.networking.v1.PlayerLookup",
                "net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents",
                "net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents$Disconnect",
                "net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents$Init",
                "net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents$Join",
                "net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking",
                "net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking$Context",
                "net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking$PlayPayloadHandler");
    }

    @Test
    void registersPayloadBeforeReceiverAndRejectsDuplicates() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                "loaderbridge", "contract_" + Long.toUnsignedString(System.nanoTime()));
        CustomPacketPayload.Type<TestPayload> type = new CustomPacketPayload.Type<>(id);

        assertThatThrownBy(() -> ServerPlayNetworking.registerGlobalReceiver(type,
                (payload, context) -> { }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LB-NET-003");

        PayloadTypeRegistry.playC2S().register(type, CODEC);
        assertThat(ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> { })).isTrue();
        assertThat(ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> { })).isFalse();
        assertThat(ServerPlayNetworking.getGlobalReceivers()).contains(id);
        assertThat(ServerPlayNetworking.unregisterGlobalReceiver(id)).isNotNull();

        assertThatThrownBy(() -> PayloadTypeRegistry.playC2S().register(type, CODEC))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LB-NET-002");
    }

    @Test
    void packetByteBufHelpersPreserveFabricSemantics() {
        var buffer = PacketByteBufs.create();
        buffer.writeUtf("loaderbridge");
        var copy = PacketByteBufs.copy(buffer);
        assertThat(copy.readUtf()).isEqualTo("loaderbridge");
        assertThat(PacketByteBufs.empty().readableBytes()).isZero();
        assertThatThrownBy(() -> PacketByteBufs.copy(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("ByteBuf cannot be null");
    }

    private record TestPayload(String value) implements CustomPacketPayload {
        private static final Type<TestPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath("loaderbridge", "test"));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
