package dev.loaderbridge.fabric.api.object.builder;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.village.WandererTradesEvent;

/** Places the binary compatibility classes in Forge's transformed game layer. */
@Mod("loaderbridge_fabric_object_builder_api_v1")
public final class FabricObjectBuilderBridgeMod {
    public FabricObjectBuilderBridgeMod() {
        MinecraftForge.EVENT_BUS.addListener(this::onVillagerTrades);
        MinecraftForge.EVENT_BUS.addListener(this::onWandererTrades);
    }

    private void onVillagerTrades(VillagerTradesEvent event) { BridgeTradeOffers.apply(event); }
    private void onWandererTrades(WandererTradesEvent event) { BridgeTradeOffers.apply(event); }
}
