package dev.loaderbridge.fabric.api.registry;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.event.registry.DynamicRegistryView;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;

/** Fabric view backed by the mutable registries created for one data-loading layer. */
public final class DynamicRegistryViewBridge implements DynamicRegistryView {
    private final Map<ResourceKey<? extends Registry<?>>, Registry<?>> registries;

    public DynamicRegistryViewBridge(
            Map<ResourceKey<? extends Registry<?>>, Registry<?>> registries) {
        this.registries = Map.copyOf(registries);
    }

    @Override
    public RegistryAccess.Frozen asDynamicRegistryManager() {
        return new RegistryAccess.Frozen() {
            @Override
            public <E> Optional<Registry<E>> registry(
                    ResourceKey<? extends Registry<? extends E>> key) {
                return DynamicRegistryViewBridge.this.getOptional(key);
            }

            @Override
            public Stream<RegistryAccess.RegistryEntry<?>> registries() {
                return DynamicRegistryViewBridge.this.registries.entrySet().stream()
                        .map(DynamicRegistryViewBridge::entry);
            }
        };
    }

    @Override
    public Stream<Registry<?>> stream() {
        return registries.values().stream();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<Registry<T>> getOptional(
            ResourceKey<? extends Registry<? extends T>> registryRef) {
        return Optional.ofNullable((Registry<T>) registries.get(registryRef));
    }

    @Override
    public <T> void registerEntryAdded(
            ResourceKey<? extends Registry<? extends T>> registryRef,
            RegistryEntryAddedCallback<T> callback) {
        getOptional(registryRef).ifPresent(registry ->
                RegistryEntryAddedCallback.event(registry).register(callback));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static RegistryAccess.RegistryEntry<?> entry(
            Map.Entry<ResourceKey<? extends Registry<?>>, Registry<?>> entry) {
        return new RegistryAccess.RegistryEntry((ResourceKey) entry.getKey(), entry.getValue());
    }
}
