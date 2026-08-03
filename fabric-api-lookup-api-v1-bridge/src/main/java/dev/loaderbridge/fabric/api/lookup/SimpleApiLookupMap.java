package dev.loaderbridge.fabric.api.lookup;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.fabricmc.fabric.api.lookup.v1.custom.ApiLookupMap;
import net.minecraft.resources.ResourceLocation;

/** Ordered type-safe custom lookup registry. */
public final class SimpleApiLookupMap<L> implements ApiLookupMap<L> {
    private final LookupConstructor<L> constructor;
    private final Map<ResourceLocation, Entry<L>> lookups = new LinkedHashMap<>();

    public SimpleApiLookupMap(LookupConstructor<L> constructor) { this.constructor = constructor; }

    @Override
    public synchronized L getLookup(ResourceLocation id, Class<?> apiClass, Class<?> contextClass) {
        Objects.requireNonNull(id); Objects.requireNonNull(apiClass); Objects.requireNonNull(contextClass);
        Entry<L> current = lookups.get(id);
        if (current == null) {
            L lookup = Objects.requireNonNull(constructor.get(id, apiClass, contextClass));
            lookups.put(id, new Entry<>(apiClass, contextClass, lookup));
            return lookup;
        }
        if (current.apiClass != apiClass || current.contextClass != contextClass) {
            throw new IllegalArgumentException("Lookup " + id + " is already registered with different types");
        }
        return current.lookup;
    }

    @Override
    public synchronized Iterator<L> iterator() {
        return List.copyOf(lookups.values().stream().map(Entry::lookup).toList()).iterator();
    }

    private record Entry<L>(Class<?> apiClass, Class<?> contextClass, L lookup) { }
}
