package dev.loaderbridge.fabric.api.content.registry;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.brewing.BrewingRecipeRegisterEvent;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("loaderbridge_fabric_content_registries_v0")
public final class FabricContentRegistriesBridgeMod {
    public FabricContentRegistriesBridgeMod() {
        MinecraftForge.EVENT_BUS.addListener(this::onFuel);
        MinecraftForge.EVENT_BUS.addListener(this::onBrewingRecipes);
        MinecraftForge.EVENT_BUS.addListener(this::onTagsUpdated);
    }

    private void onFuel(FurnaceFuelBurnTimeEvent event) {
        BridgeContentRegistries.customFuel(event.getItemStack().getItem())
                .ifPresent(event::setBurnTime);
    }

    private void onBrewingRecipes(BrewingRecipeRegisterEvent event) {
        net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder.BUILD
                .invoker().build(event.getBuilder());
    }

    private void onTagsUpdated(TagsUpdatedEvent event) {
        BridgeContentRegistries.refreshTags();
    }
}
