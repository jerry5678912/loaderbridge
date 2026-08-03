package dev.loaderbridge.fabric.api.lookup;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.resources.ResourceLocation;

/** Owns unique lookup instances and enforces their generic runtime types. */
public final class BlockApiLookupRegistry {
    private static final Map<ResourceLocation, LookupEntry> LOOKUPS = new ConcurrentHashMap<>();

    private BlockApiLookupRegistry() {
    }

    @SuppressWarnings("unchecked")
    public static <A, C> BlockApiLookup<A, C> get(ResourceLocation id, Class<A> apiClass,
            Class<C> contextClass) {
        Objects.requireNonNull(id, "Lookup id may not be null.");
        Objects.requireNonNull(apiClass, "API class may not be null.");
        Objects.requireNonNull(contextClass, "Context class may not be null.");
        LookupEntry entry = LOOKUPS.compute(id, (key, current) -> {
            if (current == null) {
                return new LookupEntry(apiClass, contextClass,
                        new SimpleBlockApiLookup<>(key, apiClass, contextClass));
            }
            if (current.apiClass != apiClass || current.contextClass != contextClass) {
                throw new IllegalArgumentException("Lookup " + key
                        + " is already registered with different API or context classes");
            }
            return current;
        });
        return (BlockApiLookup<A, C>) entry.lookup;
    }

    private record LookupEntry(Class<?> apiClass, Class<?> contextClass,
            BlockApiLookup<?, ?> lookup) {
    }
}
