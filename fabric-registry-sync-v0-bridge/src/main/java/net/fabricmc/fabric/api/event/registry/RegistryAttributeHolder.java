package net.fabricmc.fabric.api.event.registry;

import dev.loaderbridge.fabric.api.registry.RegistryAttributeStore;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/** Fabric registry-attribute contract backed by LoaderBridge state. */
public interface RegistryAttributeHolder {
    static RegistryAttributeHolder get(ResourceKey<?> registryKey) {
        return RegistryAttributeStore.get(registryKey);
    }

    static RegistryAttributeHolder get(Registry<?> registry) {
        return get(registry.key());
    }

    RegistryAttributeHolder addAttribute(RegistryAttribute attribute);

    boolean hasAttribute(RegistryAttribute attribute);
}
