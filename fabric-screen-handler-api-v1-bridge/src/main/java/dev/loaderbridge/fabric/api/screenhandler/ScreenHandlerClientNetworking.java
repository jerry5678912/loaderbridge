package dev.loaderbridge.fabric.api.screenhandler;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ScreenHandlerClientNetworking {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            "fabric-screen-handler-api-v1/client");

    static void registerReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(
                ExtendedOpenScreenPayload.TYPE, (payload, context) -> open(payload));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <D> void open(ExtendedOpenScreenPayload<D> payload) {
        var menuType = BuiltInRegistries.MENU.get(payload.menuId());
        if (menuType == null || payload.data() == null) {
            LOGGER.warn("LB-SCREEN-003: unknown screen handler ID: {}", payload.menuId());
            return;
        }
        if (!(menuType instanceof ExtendedScreenHandlerType extended)) {
            LOGGER.warn("LB-SCREEN-004: received extended opening packet for non-extended "
                    + "screen handler {}", payload.menuId());
            return;
        }
        Minecraft client = Minecraft.getInstance();
        var player = client.player;
        if (player == null) {
            LOGGER.warn("LB-SCREEN-005: cannot open screen handler {} without a client player",
                    payload.menuId());
            return;
        }
        java.util.Optional<MenuScreens.ScreenConstructor> constructor = (java.util.Optional)
                MenuScreens.getScreenFactory(menuType, client,
                        payload.syncId(), payload.title());
        if (constructor.isEmpty()) {
            LOGGER.warn("LB-SCREEN-006: no screen registered for screen handler {}",
                    payload.menuId());
            return;
        }
        AbstractContainerMenu menu = extended.create(
                payload.syncId(), player.getInventory(), payload.data());
        Screen screen = constructor.get().create(menu, player.getInventory(), payload.title());
        player.containerMenu = ((MenuAccess<AbstractContainerMenu>) screen).getMenu();
        client.setScreen(screen);
    }

    private ScreenHandlerClientNetworking() { }
}
