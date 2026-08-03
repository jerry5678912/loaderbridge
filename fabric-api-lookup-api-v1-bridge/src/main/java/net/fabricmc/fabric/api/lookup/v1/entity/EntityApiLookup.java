package net.fabricmc.fabric.api.lookup.v1.entity;

import dev.loaderbridge.fabric.api.lookup.EntityApiLookupRegistry;
import java.util.function.BiFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/** Binary-compatible Fabric entity API lookup contract. */
public interface EntityApiLookup<A, C> {
    static <A, C> EntityApiLookup<A, C> get(ResourceLocation id, Class<A> apiClass,
            Class<C> contextClass) {
        return EntityApiLookupRegistry.get(id, apiClass, contextClass);
    }

    A find(Entity entity, C context);

    void registerSelf(EntityType<?>... entityTypes);

    @SuppressWarnings("unchecked")
    default <T extends Entity> void registerForType(BiFunction<T, C, A> provider,
            EntityType<T> entityType) {
        registerForTypes((entity, context) -> provider.apply((T) entity, context), entityType);
    }

    void registerForTypes(EntityApiProvider<A, C> provider, EntityType<?>... entityTypes);

    void registerFallback(EntityApiProvider<A, C> fallbackProvider);

    ResourceLocation getId();

    Class<A> apiClass();

    Class<C> contextClass();

    EntityApiProvider<A, C> getProvider(EntityType<?> entityType);

    @FunctionalInterface
    interface EntityApiProvider<A, C> {
        A find(Entity entity, C context);
    }
}
