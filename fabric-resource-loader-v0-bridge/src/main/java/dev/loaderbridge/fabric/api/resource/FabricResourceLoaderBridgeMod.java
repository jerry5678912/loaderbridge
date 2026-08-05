package dev.loaderbridge.fabric.api.resource;

import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("loaderbridge_fabric_resource_loader_v0")
public final class FabricResourceLoaderBridgeMod {
    @SuppressWarnings("removal")
    public FabricResourceLoaderBridgeMod() {
        MinecraftForge.EVENT_BUS.addListener(this::onServerReloadListeners);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onAddPackFinders);
    }

    private void onAddPackFinders(AddPackFindersEvent event) {
        event.addRepositorySource(consumer -> ResourceManagerHelperImpl.loadBuiltinPacks(
                event.getPackType(), consumer));
    }

    @SuppressWarnings("removal")
    private void onServerReloadListeners(AddReloadListenerEvent event) {
        ResourceManagerHelperImpl.get(PackType.SERVER_DATA)
                .listeners(event.getRegistryAccess().freeze()).forEach(event::addListener);
    }
}
