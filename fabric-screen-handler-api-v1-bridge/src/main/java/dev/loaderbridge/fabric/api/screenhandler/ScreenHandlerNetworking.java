package dev.loaderbridge.fabric.api.screenhandler;

import java.util.Objects;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class ScreenHandlerNetworking {
    static void registerPayload() {
        PayloadTypeRegistry.playS2C().register(
                ExtendedOpenScreenPayload.TYPE, ExtendedOpenScreenPayload.CODEC);
    }

    @SuppressWarnings("unchecked")
    public static <D> void sendOpenPacket(ServerPlayer player,
            ExtendedScreenHandlerFactory<D> factory, AbstractContainerMenu menu, int syncId) {
        Objects.requireNonNull(player, "player is null");
        Objects.requireNonNull(factory, "factory is null");
        Objects.requireNonNull(menu, "handler is null");
        var menuId = BuiltInRegistries.MENU.getKey(menu.getType());
        if (menuId == null) {
            throw new IllegalStateException("LB-SCREEN-001: extended menu type is not registered");
        }
        if (!(menu.getType() instanceof ExtendedScreenHandlerType<?, ?> extended)) {
            throw new IllegalArgumentException(
                    "LB-SCREEN-002: extended factory created non-extended menu " + menuId);
        }
        var codec = (net.minecraft.network.codec.StreamCodec<
                ? super net.minecraft.network.RegistryFriendlyByteBuf, D>) extended.getPacketCodec();
        D data = factory.getScreenOpeningData(player);
        ServerPlayNetworking.send(player, new ExtendedOpenScreenPayload<>(menuId, syncId,
                factory.getDisplayName(), codec, data));
    }

    private ScreenHandlerNetworking() { }
}
