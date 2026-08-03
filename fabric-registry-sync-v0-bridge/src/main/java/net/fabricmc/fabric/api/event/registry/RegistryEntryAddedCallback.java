package net.fabricmc.fabric.api.event.registry;

import dev.loaderbridge.fabric.api.registry.RegistryEventDispatcher;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

@FunctionalInterface
public interface RegistryEntryAddedCallback<T> {
    void onEntryAdded(int rawId, ResourceLocation id, T object);

    static <T> Event<RegistryEntryAddedCallback<T>> event(Registry<T> registry) {
        return RegistryEventDispatcher.entryAdded(registry);
    }
}
