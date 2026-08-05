package net.fabricmc.fabric.api.tag.convention.v2;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public final class TagUtil {
    public static final String C_TAG_NAMESPACE = "c";
    public static final String FABRIC_TAG_NAMESPACE = "fabric";

    private TagUtil() { }

    public static <T> boolean isIn(TagKey<T> tagKey, T entry) {
        return isIn(null, tagKey, entry);
    }

    @SuppressWarnings("unchecked")
    public static <T> boolean isIn(RegistryAccess registryAccess, TagKey<T> tagKey, T entry) {
        Objects.requireNonNull(tagKey);
        Objects.requireNonNull(entry);
        Optional<? extends Registry<?>> registry = registryAccess != null
                ? registryAccess.registry(tagKey.registry())
                : BuiltInRegistries.REGISTRY.getOptional(tagKey.registry().location())
                        .map(value -> (Registry<?>) value);
        if (registry.isEmpty() || !tagKey.isFor(registry.get().key())) return false;
        Registry<T> typedRegistry = (Registry<T>) registry.get();
        Optional<ResourceKey<T>> key = typedRegistry.getResourceKey(entry);
        return key.flatMap(typedRegistry::getHolder)
                .map(holder -> holder.is(tagKey)).orElse(false);
    }
}
