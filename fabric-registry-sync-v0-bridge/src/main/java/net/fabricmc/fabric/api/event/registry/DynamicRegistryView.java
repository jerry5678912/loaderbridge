package net.fabricmc.fabric.api.event.registry;

import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;

/** Read-only view of the dynamic registries in the layer currently being loaded. */
public interface DynamicRegistryView {
    RegistryAccess.Frozen asDynamicRegistryManager();

    Stream<Registry<?>> stream();

    <T> Optional<Registry<T>> getOptional(
            ResourceKey<? extends Registry<? extends T>> registryRef);

    <T> void registerEntryAdded(
            ResourceKey<? extends Registry<? extends T>> registryRef,
            RegistryEntryAddedCallback<T> callback);
}
