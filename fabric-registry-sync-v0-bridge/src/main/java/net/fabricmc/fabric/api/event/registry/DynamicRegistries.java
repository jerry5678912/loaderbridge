package net.fabricmc.fabric.api.event.registry;

import com.mojang.serialization.Codec;
import dev.loaderbridge.fabric.api.registry.DynamicRegistryRuntime;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;

/** Fabric's public data-driven registry registration contract. */
public final class DynamicRegistries {
    private DynamicRegistries() {
    }

    public static List<RegistryDataLoader.RegistryData<?>> getDynamicRegistries() {
        return DynamicRegistryRuntime.descriptors();
    }

    public static <T> void register(ResourceKey<? extends Registry<T>> key, Codec<T> codec) {
        DynamicRegistryRuntime.register(key, codec);
    }

    public static <T> void registerSynced(ResourceKey<? extends Registry<T>> key,
            Codec<T> codec, SyncOption... options) {
        registerSynced(key, codec, codec, options);
    }

    public static <T> void registerSynced(ResourceKey<? extends Registry<T>> key,
            Codec<T> dataCodec, Codec<T> networkCodec, SyncOption... options) {
        DynamicRegistryRuntime.register(key, dataCodec);
        DynamicRegistryRuntime.addSynced(key, networkCodec, options);
    }

    public enum SyncOption {
        SKIP_WHEN_EMPTY
    }
}
