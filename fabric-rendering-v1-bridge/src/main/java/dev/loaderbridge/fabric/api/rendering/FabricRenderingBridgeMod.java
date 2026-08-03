package dev.loaderbridge.fabric.api.rendering;

import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("loaderbridge_fabric_rendering_v1")
public final class FabricRenderingBridgeMod {
    @SuppressWarnings("removal")
    public FabricRenderingBridgeMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerBlockColors);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerItemColors);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerModelLayers);
    }

    private void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        ((ColorProviderRegistry.BlockRegistry) ColorProviderRegistry.BLOCK).registerTo(event);
    }

    private void registerItemColors(RegisterColorHandlersEvent.Item event) {
        ((ColorProviderRegistry.ItemRegistry) ColorProviderRegistry.ITEM).registerTo(event);
    }

    private void registerModelLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        EntityModelLayerRegistry.registerTo(event);
    }
}
