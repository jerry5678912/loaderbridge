package dev.loaderbridge.fabric.api.itemgroup;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("loaderbridge_fabric_item_group_api_v1")
public final class FabricItemGroupBridgeMod {
    @SuppressWarnings("removal")
    public FabricItemGroupBridgeMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onBuildContents);
    }

    private void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        BridgeItemGroupEvents.apply(event);
    }
}
