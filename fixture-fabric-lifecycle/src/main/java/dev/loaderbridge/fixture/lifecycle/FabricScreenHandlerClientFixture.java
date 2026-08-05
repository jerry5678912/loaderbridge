package dev.loaderbridge.fixture.lifecycle;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class FabricScreenHandlerClientFixture implements ClientModInitializer {
    @Override public void onInitializeClient() {
        MenuScreens.register(FabricScreenHandlerFixture.TYPE, FixtureScreen::new);
    }

    private static final class FixtureScreen extends AbstractContainerScreen<
            FabricScreenHandlerFixture.FixtureMenu> {
        private int ticks;

        private FixtureScreen(FabricScreenHandlerFixture.FixtureMenu menu,
                Inventory inventory, Component title) {
            super(menu, inventory, title);
            var data = menu.openingData();
            if (!FabricScreenHandlerFixture.LABEL.equals(data.label())
                    || data.value() != FabricScreenHandlerFixture.VALUE) {
                throw new IllegalStateException("Screen opening data did not round-trip");
            }
            System.out.println("LOADERBRIDGE_FABRIC_SCREEN_HANDLER_READY label="
                    + data.label() + ",value=" + data.value());
        }

        @Override protected void renderBg(GuiGraphics graphics, float partialTick,
                int mouseX, int mouseY) {
            graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight,
                    0xFF202830);
        }

        @Override protected void containerTick() {
            super.containerTick();
            if (++ticks == 2) onClose();
        }
    }
}
