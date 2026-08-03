package net.fabricmc.fabric.api.lookup.v1.custom;

import dev.loaderbridge.fabric.api.lookup.SimpleApiLookupMap;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Type-safe custom lookup registry contract. */
public interface ApiLookupMap<L> extends Iterable<L> {
    static <L> ApiLookupMap<L> create(LookupConstructor<L> constructor) {
        return new SimpleApiLookupMap<>(Objects.requireNonNull(constructor,
                "Lookup factory may not be null."));
    }

    L getLookup(ResourceLocation id, Class<?> apiClass, Class<?> contextClass);

    @FunctionalInterface
    interface LookupConstructor<L> {
        L get(ResourceLocation id, Class<?> apiClass, Class<?> contextClass);
    }

    @Deprecated(forRemoval = true)
    static <L> ApiLookupMap<L> create(LookupFactory<L> factory) {
        Objects.requireNonNull(factory, "Lookup factory may not be null.");
        return create((id, apiClass, contextClass) -> factory.get(apiClass, contextClass));
    }

    @Deprecated(forRemoval = true)
    @FunctionalInterface
    interface LookupFactory<L> {
        L get(Class<?> apiClass, Class<?> contextClass);
    }
}
