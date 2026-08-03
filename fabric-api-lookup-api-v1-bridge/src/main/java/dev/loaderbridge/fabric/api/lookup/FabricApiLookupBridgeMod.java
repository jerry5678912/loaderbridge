package dev.loaderbridge.fabric.api.lookup;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;

/** Places lookup compatibility classes in Forge's transformed game layer. */
@Mod("loaderbridge_fabric_api_lookup_api_v1")
public final class FabricApiLookupBridgeMod {
    public FabricApiLookupBridgeMod() {
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
    }

    private void onServerStarted(ServerStartedEvent event) {
        EntityApiLookupRegistry.validateSelfProviders(event.getServer());
    }
}
