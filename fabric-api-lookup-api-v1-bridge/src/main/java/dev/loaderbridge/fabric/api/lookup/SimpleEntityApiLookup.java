package dev.loaderbridge.fabric.api.lookup;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/** Provider-first Fabric entity lookup with deferred self validation. */
final class SimpleEntityApiLookup<A, C> implements EntityApiLookup<A, C> {
    private final ResourceLocation id;
    private final Class<A> apiClass;
    private final Class<C> contextClass;
    private final Map<EntityType<?>, EntityApiProvider<A, C>> providers = new ConcurrentHashMap<>();
    private final List<EntityApiProvider<A, C>> fallbacks = new CopyOnWriteArrayList<>();
    private final List<EntityType<?>> pendingSelfTypes = new CopyOnWriteArrayList<>();

    @SuppressWarnings("unchecked")
    SimpleEntityApiLookup(ResourceLocation id, Class<?> apiClass, Class<?> contextClass) {
        this.id = id; this.apiClass = (Class<A>) apiClass; this.contextClass = (Class<C>) contextClass;
    }

    @Override public A find(Entity entity, C context) {
        Objects.requireNonNull(entity, "Entity may not be null.");
        EntityApiProvider<A, C> provider = providers.get(entity.getType());
        if (provider != null) {
            A result = provider.find(entity, context);
            if (result != null) return result;
        }
        for (EntityApiProvider<A, C> fallback : fallbacks) {
            A result = fallback.find(entity, context);
            if (result != null) return result;
        }
        return null;
    }

    @Override @SuppressWarnings("unchecked") public void registerSelf(EntityType<?>... types) {
        for (EntityType<?> type : checked(types)) pendingSelfTypes.add(type);
        registerForTypes((entity, context) -> apiClass.isInstance(entity) ? (A) entity : null, types);
    }

    void validateSelfProviders(MinecraftServer server) {
        for (EntityType<?> type : pendingSelfTypes) {
            Entity entity = Objects.requireNonNull(type.create(server.overworld()),
                    "Instantiated entity may not be null.");
            if (!apiClass.isInstance(entity)) {
                throw new IllegalArgumentException("API class " + apiClass.getCanonicalName()
                        + " is not assignable from entity class " + entity.getClass().getCanonicalName());
            }
        }
        pendingSelfTypes.clear();
    }

    @Override public void registerForTypes(EntityApiProvider<A, C> provider, EntityType<?>... types) {
        Objects.requireNonNull(provider, "EntityApiProvider may not be null.");
        for (EntityType<?> type : checked(types)) providers.putIfAbsent(type, provider);
    }

    private static EntityType<?>[] checked(EntityType<?>[] types) {
        if (types.length == 0) throw new IllegalArgumentException("Must register at least one entity type");
        for (EntityType<?> type : types) Objects.requireNonNull(type, "Encountered null entity type");
        return types;
    }

    @Override public void registerFallback(EntityApiProvider<A, C> provider) { fallbacks.add(Objects.requireNonNull(provider)); }
    @Override public ResourceLocation getId() { return id; }
    @Override public Class<A> apiClass() { return apiClass; }
    @Override public Class<C> contextClass() { return contextClass; }
    @Override public EntityApiProvider<A, C> getProvider(EntityType<?> type) { return providers.get(type); }
}
