package dev.loaderbridge.fabric.api.screenhandler;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod("loaderbridge_fabric_screen_handler")
public final class FabricScreenHandlerBridgeMod {
    public FabricScreenHandlerBridgeMod() {
        ScreenHandlerNetworking.registerPayload();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ScreenHandlerClientNetworking.registerReceiver();
        }
    }
}
