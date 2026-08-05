package dev.loaderbridge.fixture.lifecycle;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

final class FabricScreenHandlerFixture {
    static final String LABEL = "loaderbridge-screen";
    static final int VALUE = 37;
    static final StreamCodec<RegistryFriendlyByteBuf, OpeningData> CODEC = StreamCodec.of(
            (buffer, data) -> {
                buffer.writeUtf(data.label());
                buffer.writeVarInt(data.value());
            }, buffer -> new OpeningData(buffer.readUtf(), buffer.readVarInt()));
    static final ExtendedScreenHandlerType<FixtureMenu, OpeningData> TYPE = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation.fromNamespaceAndPath("loaderbridge", "extended_fixture"),
            new ExtendedScreenHandlerType<>(FixtureMenu::new, CODEC));

    static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                handler.getPlayer().openMenu(new ExtendedScreenHandlerFactory<OpeningData>() {
                    @Override public OpeningData getScreenOpeningData(
                            net.minecraft.server.level.ServerPlayer player) {
                        return new OpeningData(LABEL, VALUE);
                    }

                    @Override public Component getDisplayName() {
                        return Component.literal("LoaderBridge Extended Fixture");
                    }

                    @Override public FixtureMenu createMenu(int syncId, Inventory inventory,
                            Player player) {
                        return new FixtureMenu(syncId, inventory,
                                new OpeningData(LABEL, VALUE));
                    }
                }));
    }

    record OpeningData(String label, int value) { }

    static final class FixtureMenu extends AbstractContainerMenu {
        private final OpeningData openingData;

        FixtureMenu(int syncId, Inventory inventory, OpeningData openingData) {
            super(TYPE, syncId);
            this.openingData = openingData;
        }

        OpeningData openingData() {
            return openingData;
        }

        @Override public ItemStack quickMoveStack(Player player, int slot) {
            return ItemStack.EMPTY;
        }

        @Override public boolean stillValid(Player player) {
            return true;
        }
    }

    private FabricScreenHandlerFixture() { }
}
