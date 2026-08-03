package dev.loaderbridge.fabric.api.registry;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;

/** Mutable registry descriptors consumed by the WorldLoader and sync Mixins. */
public final class DynamicRegistryRuntime {
    private static final List<RegistryDataLoader.RegistryData<?>> DESCRIPTORS =
            new ArrayList<>(RegistryDataLoader.WORLDGEN_REGISTRIES);
    private static final Set<ResourceKey<? extends Registry<?>>> KEYS = new HashSet<>();
    private static final Set<ResourceKey<? extends Registry<?>>> SKIP_WHEN_EMPTY = new HashSet<>();

    static {
        RegistryDataLoader.WORLDGEN_REGISTRIES.forEach(data -> KEYS.add(data.key()));
    }

    private DynamicRegistryRuntime() {
    }

    public static synchronized List<RegistryDataLoader.RegistryData<?>> descriptors() {
        return List.copyOf(DESCRIPTORS);
    }

    public static synchronized <T> void register(ResourceKey<? extends Registry<T>> key,
            Codec<T> codec) {
        Objects.requireNonNull(key, "Registry key cannot be null");
        Objects.requireNonNull(codec, "Codec cannot be null");
        if (!KEYS.add(castKey(key))) {
            throw new IllegalArgumentException("Dynamic registry " + key + " has already been registered!");
        }
        DESCRIPTORS.add(new RegistryDataLoader.RegistryData<>(key, codec, false));
    }

    public static synchronized <T> void addSynced(ResourceKey<? extends Registry<T>> key,
            Codec<T> networkCodec, DynamicRegistries.SyncOption... options) {
        Objects.requireNonNull(key, "Registry key cannot be null");
        Objects.requireNonNull(networkCodec, "Network codec cannot be null");
        Objects.requireNonNull(options, "Options cannot be null");
        RegistryDataLoader.SYNCHRONIZED_REGISTRIES.add(
                new RegistryDataLoader.RegistryData<>(key, networkCodec, false));
        RegistrySynchronization.NETWORKABLE_REGISTRIES.add(castKey(key));
        for (DynamicRegistries.SyncOption option : options) {
            if (option == DynamicRegistries.SyncOption.SKIP_WHEN_EMPTY) {
                SKIP_WHEN_EMPTY.add(castKey(key));
            }
        }
    }

    public static synchronized boolean skipWhenEmpty(ResourceKey<? extends Registry<?>> key) {
        return SKIP_WHEN_EMPTY.contains(key);
    }

    @SuppressWarnings("unchecked")
    private static ResourceKey<? extends Registry<?>> castKey(ResourceKey<?> key) {
        return (ResourceKey<? extends Registry<?>>) key;
    }
}
