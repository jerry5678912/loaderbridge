package net.fabricmc.fabric.api.event.registry;

import com.mojang.serialization.Lifecycle;
import java.util.EnumSet;
import net.minecraft.core.DefaultedMappedRegistry;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/** Independently implemented Fabric custom-registry builder for Forge's game layer. */
public final class FabricRegistryBuilder<T, R extends WritableRegistry<T>> {
    public static <T, R extends WritableRegistry<T>> FabricRegistryBuilder<T, R> from(R registry) {
        return new FabricRegistryBuilder<>(registry);
    }

    public static <T> FabricRegistryBuilder<T, MappedRegistry<T>> createSimple(
            ResourceKey<Registry<T>> registryKey) {
        return from(new MappedRegistry<>(registryKey, Lifecycle.stable(), false));
    }

    public static <T> FabricRegistryBuilder<T, DefaultedMappedRegistry<T>> createDefaulted(
            ResourceKey<Registry<T>> registryKey, ResourceLocation defaultId) {
        return from(new DefaultedMappedRegistry<>(defaultId.toString(), registryKey,
                Lifecycle.stable(), false));
    }

    @Deprecated
    public static <T> FabricRegistryBuilder<T, MappedRegistry<T>> createSimple(
            Class<T> type, ResourceLocation registryId) {
        return createSimple(ResourceKey.createRegistryKey(registryId));
    }

    @Deprecated
    public static <T> FabricRegistryBuilder<T, DefaultedMappedRegistry<T>> createDefaulted(
            Class<T> type, ResourceLocation registryId, ResourceLocation defaultId) {
        return createDefaulted(ResourceKey.createRegistryKey(registryId), defaultId);
    }

    private final R registry;
    private final EnumSet<RegistryAttribute> attributes = EnumSet.of(RegistryAttribute.MODDED);

    private FabricRegistryBuilder(R registry) {
        this.registry = java.util.Objects.requireNonNull(registry, "registry");
    }

    public FabricRegistryBuilder<T, R> attribute(RegistryAttribute attribute) {
        attributes.add(java.util.Objects.requireNonNull(attribute, "attribute"));
        return this;
    }

    public R buildAndRegister() {
        ResourceKey<?> key = registry.key();
        attributes.forEach(attribute -> RegistryAttributeHolder.get(key).addAttribute(attribute));
        registerRoot(key, registry);
        return registry;
    }

    @SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
    private static void registerRoot(ResourceKey<?> key, WritableRegistry<?> registry) {
        Registry root = BuiltInRegistries.REGISTRY;
        if (root instanceof MappedRegistry<?> mapped) {
            mapped.unfreeze();
        }
        Registry.register(root, (ResourceKey) key, registry);
    }
}
