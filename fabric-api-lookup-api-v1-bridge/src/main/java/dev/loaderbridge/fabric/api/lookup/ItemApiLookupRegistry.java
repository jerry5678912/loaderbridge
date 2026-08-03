package dev.loaderbridge.fabric.api.lookup;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.minecraft.resources.ResourceLocation;

/** Owns unique typed item lookup instances. */
public final class ItemApiLookupRegistry {
    private static final Map<ResourceLocation, Entry> LOOKUPS = new ConcurrentHashMap<>();
    private ItemApiLookupRegistry() { }

    @SuppressWarnings("unchecked")
    public static <A, C> ItemApiLookup<A, C> get(ResourceLocation id, Class<A> apiClass,
            Class<C> contextClass) {
        Objects.requireNonNull(id); Objects.requireNonNull(apiClass); Objects.requireNonNull(contextClass);
        Entry entry = LOOKUPS.compute(id, (key, current) -> checked(key, apiClass, contextClass, current));
        return (ItemApiLookup<A, C>) entry.lookup;
    }

    private static Entry checked(ResourceLocation id, Class<?> api, Class<?> context, Entry current) {
        if (current == null) return new Entry(api, context, new SimpleItemApiLookup<>(id, api, context));
        if (current.apiClass != api || current.contextClass != context) {
            throw new IllegalArgumentException("Lookup " + id + " is already registered with different types");
        }
        return current;
    }

    private record Entry(Class<?> apiClass, Class<?> contextClass, ItemApiLookup<?, ?> lookup) { }
}
