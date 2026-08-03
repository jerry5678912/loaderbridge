package net.fabricmc.fabric.api.registry;

import dev.loaderbridge.fabric.api.content.registry.BridgeContentRegistries;
import net.fabricmc.fabric.api.util.Item2ObjectMap;

/** Fabric-compatible composter chance registry. */
public interface CompostingChanceRegistry extends Item2ObjectMap<Float> {
    CompostingChanceRegistry INSTANCE = BridgeContentRegistries.composting();
}
