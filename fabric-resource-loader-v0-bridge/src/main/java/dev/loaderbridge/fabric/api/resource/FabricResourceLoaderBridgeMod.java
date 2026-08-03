package dev.loaderbridge.fabric.api.resource;

import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("loaderbridge_fabric_resource_loader_v0")
public final class FabricResourceLoaderBridgeMod {
    public FabricResourceLoaderBridgeMod() {
        MinecraftForge.EVENT_BUS.addListener(this::onServerReloadListeners);
    }

    @SuppressWarnings("removal")
    private void onServerReloadListeners(AddReloadListenerEvent event) {
        ResourceManagerHelperImpl.get(PackType.SERVER_DATA)
                .listeners(event.getRegistryAccess().freeze()).forEach(event::addListener);
    }
}
