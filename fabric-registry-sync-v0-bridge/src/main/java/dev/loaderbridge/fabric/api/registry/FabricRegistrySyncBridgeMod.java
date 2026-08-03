package dev.loaderbridge.fabric.api.registry;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;

/** Places registry compatibility classes and their early Mixin in Forge's game layer. */
@Mod("loaderbridge_fabric_registry_sync_v0")
public final class FabricRegistrySyncBridgeMod {
    public FabricRegistrySyncBridgeMod() {
        MinecraftForge.EVENT_BUS.addListener(ForgeRegistryRemapBridge::onIdMapping);
    }
}
