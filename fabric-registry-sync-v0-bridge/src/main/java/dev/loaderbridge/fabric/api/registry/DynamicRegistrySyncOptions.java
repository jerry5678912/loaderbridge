package dev.loaderbridge.fabric.api.registry;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;

/** Sync-option state kept independent from Minecraft's bootstrapped registry descriptors. */
public final class DynamicRegistrySyncOptions {
    private static final Set<ResourceKey<? extends Registry<?>>> SKIP_WHEN_EMPTY =
            new HashSet<>();

    private DynamicRegistrySyncOptions() {
    }

    public static synchronized void markSkipWhenEmpty(
            ResourceKey<? extends Registry<?>> key) {
        SKIP_WHEN_EMPTY.add(key);
    }

    public static synchronized boolean skipWhenEmpty(
            ResourceKey<? extends Registry<?>> key) {
        return SKIP_WHEN_EMPTY.contains(key);
    }

    public static boolean shouldSkipEmpty(
            ResourceKey<? extends Registry<?>> key, RegistryAccess access) {
        return skipWhenEmpty(key) && access.registry(key).map(Registry::size).orElse(0) == 0;
    }

    public static boolean shouldSkipEmpty(
            ResourceKey<? extends Registry<?>> key, Registry<?> registry) {
        return skipWhenEmpty(key) && registry.size() == 0;
    }
}
