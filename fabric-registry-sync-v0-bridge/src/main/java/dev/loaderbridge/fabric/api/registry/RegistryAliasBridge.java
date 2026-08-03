package dev.loaderbridge.fabric.api.registry;

import net.minecraft.core.DefaultedRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

/** Maps Fabric's registry-alias ABI onto Forge's authoritative registry. */
public final class RegistryAliasBridge {
    private RegistryAliasBridge() {
    }

    public static void addAlias(DefaultedRegistry<?> registry,
            ResourceLocation alias, ResourceLocation target) {
        ForgeRegistry<?> forgeRegistry = RegistryManager.ACTIVE.getRegistry(registry.key());
        if (forgeRegistry == null) {
            throw new IllegalStateException("LB-REGISTRY-004: Forge registry is unavailable for "
                    + registry.key().location());
        }
        forgeRegistry.addAlias(alias, target);
    }
}
