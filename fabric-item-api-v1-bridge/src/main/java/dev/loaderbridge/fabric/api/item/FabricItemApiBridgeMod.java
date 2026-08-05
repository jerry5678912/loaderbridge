package dev.loaderbridge.fabric.api.item;

import net.fabricmc.fabric.impl.item.DefaultItemComponentImpl;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("loaderbridge_fabric_item_api_v1")
public final class FabricItemApiBridgeMod {
    @SuppressWarnings("removal")
    public FabricItemApiBridgeMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        // Client Fabric initialization can run later at Forge's recipe-book event.
        DefaultItemComponentImpl.markRegistryReadyAndModify();
    }
}
