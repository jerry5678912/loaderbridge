package net.fabricmc.fabric.api.registry;

import dev.loaderbridge.fabric.api.content.registry.BridgeContentRegistries;
import net.fabricmc.fabric.api.util.Item2ObjectMap;

/** Fabric-compatible furnace fuel overrides. */
public interface FuelRegistry extends Item2ObjectMap<Integer> {
    FuelRegistry INSTANCE = BridgeContentRegistries.fuels();
}
