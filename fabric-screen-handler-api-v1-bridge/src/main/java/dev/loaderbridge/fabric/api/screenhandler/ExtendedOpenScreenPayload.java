package dev.loaderbridge.fabric.api.screenhandler;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

record ExtendedOpenScreenPayload<D>(ResourceLocation menuId, int syncId, Component title,
        StreamCodec<? super RegistryFriendlyByteBuf, D> innerCodec, D data)
        implements CustomPacketPayload {
    static final Type<ExtendedOpenScreenPayload<?>> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("fabric-screen-handler-api-v1", "open_screen"));
    static final StreamCodec<RegistryFriendlyByteBuf, ExtendedOpenScreenPayload<?>> CODEC =
            StreamCodec.of(ExtendedOpenScreenPayload::encodeUnknown,
                    ExtendedOpenScreenPayload::decode);

    @SuppressWarnings("unchecked")
    private static <D> ExtendedOpenScreenPayload<D> decode(RegistryFriendlyByteBuf buffer) {
        ResourceLocation menuId = buffer.readResourceLocation();
        int syncId = buffer.readByte();
        Component title = ComponentSerialization.STREAM_CODEC.decode(buffer);
        var menuType = BuiltInRegistries.MENU.get(menuId);
        StreamCodec<? super RegistryFriendlyByteBuf, D> codec = menuType
                instanceof ExtendedScreenHandlerType<?, ?> extended
                ? (StreamCodec<? super RegistryFriendlyByteBuf, D>) extended.getPacketCodec()
                : null;
        D data = codec == null ? null : codec.decode(buffer);
        return new ExtendedOpenScreenPayload<>(menuId, syncId, title, codec, data);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void encodeUnknown(RegistryFriendlyByteBuf buffer,
            ExtendedOpenScreenPayload<?> payload) {
        encode(buffer, (ExtendedOpenScreenPayload) payload);
    }

    private static <D> void encode(RegistryFriendlyByteBuf buffer,
            ExtendedOpenScreenPayload<D> payload) {
        buffer.writeResourceLocation(payload.menuId);
        buffer.writeVarInt(payload.syncId);
        ComponentSerialization.STREAM_CODEC.encode(buffer, payload.title);
        payload.innerCodec.encode(buffer, payload.data);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
