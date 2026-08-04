package dev.loaderbridge.fabric.api.resource.conditions;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagManager;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Runtime implementation kept outside Fabric's public API package. */
public final class BridgeResourceConditions {
    private static final Logger LOGGER = LoggerFactory.getLogger("LoaderBridge Resource Conditions");
    private static final AtomicBoolean DEFAULTS_REGISTERED = new AtomicBoolean();
    private static final ThreadLocal<Map<ResourceKey<?>, Set<ResourceLocation>>> LOADED_TAGS =
            new ThreadLocal<>();
    private static volatile FeatureFlagSet currentFeatures = FeatureFlags.DEFAULT_FLAGS;

    private static final ResourceConditionType<TrueCondition> TRUE = type("true", TrueCondition.CODEC);
    private static final ResourceConditionType<NotCondition> NOT = type("not", NotCondition.CODEC);
    private static final ResourceConditionType<AndCondition> AND = type("and", AndCondition.CODEC);
    private static final ResourceConditionType<OrCondition> OR = type("or", OrCondition.CODEC);
    private static final ResourceConditionType<ModsCondition> ALL_MODS = type("all_mods_loaded", ModsCondition.ALL_CODEC);
    private static final ResourceConditionType<ModsCondition> ANY_MODS = type("any_mods_loaded", ModsCondition.ANY_CODEC);
    private static final ResourceConditionType<TagsCondition> TAGS = type("tags_populated", TagsCondition.CODEC);
    private static final ResourceConditionType<FeaturesCondition> FEATURES = type("features_enabled", FeaturesCondition.CODEC);
    private static final ResourceConditionType<RegistryCondition> REGISTRY = type("registry_contains", RegistryCondition.CODEC);

    private BridgeResourceConditions() {}

    public static void registerDefaults() {
        if (!DEFAULTS_REGISTERED.compareAndSet(false, true)) {
            return;
        }
        ResourceConditions.register(TRUE);
        ResourceConditions.register(NOT);
        ResourceConditions.register(AND);
        ResourceConditions.register(OR);
        ResourceConditions.register(ALL_MODS);
        ResourceConditions.register(ANY_MODS);
        ResourceConditions.register(TAGS);
        ResourceConditions.register(FEATURES);
        ResourceConditions.register(REGISTRY);
    }

    public static ResourceCondition alwaysTrue() {
        return new TrueCondition();
    }

    public static void beginServerResourceReload(FeatureFlagSet enabledFeatures) {
        currentFeatures = java.util.Objects.requireNonNull(enabledFeatures, "enabledFeatures");
        LOADED_TAGS.remove();
    }

    public static void captureLoadedTags(List<TagManager.LoadResult<?>> results) {
        Map<ResourceKey<?>, Set<ResourceLocation>> loaded = new HashMap<>();
        for (TagManager.LoadResult<?> result : results) {
            loaded.put(result.key(), Set.copyOf(result.tags().keySet()));
        }
        LOADED_TAGS.set(Map.copyOf(loaded));
    }

    public static ResourceCondition not(ResourceCondition condition) {
        return new NotCondition(condition);
    }

    public static ResourceCondition and(ResourceCondition... conditions) {
        return new AndCondition(List.of(conditions));
    }

    public static ResourceCondition or(ResourceCondition... conditions) {
        return new OrCondition(List.of(conditions));
    }

    public static ResourceCondition allModsLoaded(String... modIds) {
        return new ModsCondition(List.of(modIds), true);
    }

    public static ResourceCondition anyModsLoaded(String... modIds) {
        return new ModsCondition(List.of(modIds), false);
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> ResourceCondition tagsPopulated(TagKey<T>... tags) {
        ResourceLocation registry = tags.length == 0 ? Registries.ITEM.location() : tags[0].registry().location();
        return new TagsCondition(registry, Arrays.stream(tags).map(TagKey::location).toList());
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> ResourceCondition tagsPopulated(
            ResourceKey<? extends Registry<T>> registry, TagKey<T>... tags) {
        return new TagsCondition(registry.location(), Arrays.stream(tags).map(TagKey::location).toList());
    }

    public static ResourceCondition featuresEnabled(ResourceLocation... features) {
        return new FeaturesCondition(List.of(features));
    }

    public static ResourceCondition featuresEnabled(FeatureFlag... features) {
        return new FeaturesCondition(List.copyOf(FeatureFlags.REGISTRY.toNames(FeatureFlags.REGISTRY.subset(features))));
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> ResourceCondition registryContains(ResourceKey<T>... entries) {
        ResourceLocation registry = entries.length == 0 ? Registries.ITEM.location() : entries[0].registry();
        return new RegistryCondition(registry, Arrays.stream(entries).map(ResourceKey::location).toList());
    }

    public static <T> ResourceCondition registryContains(
            ResourceKey<? extends Registry<T>> registry, ResourceLocation... entries) {
        return new RegistryCondition(registry.location(), List.of(entries));
    }

    /** Returns Fabric's permissive result on malformed conditions after reporting the parse failure. */
    public static boolean test(JsonObject json, HolderLookup.Provider registryLookup) {
        if (!json.has(ResourceConditions.CONDITIONS_KEY)) {
            return true;
        }
        var parsed = ResourceCondition.CONDITION_CODEC.parse(
                JsonOps.INSTANCE, json.get(ResourceConditions.CONDITIONS_KEY));
        if (parsed.isSuccess()) {
            return parsed.getOrThrow().test(registryLookup);
        }
        String message = parsed.error().map(error -> error.message()).orElse("unknown codec failure");
        LOGGER.error("LB-FAPI-RC-001: failed to parse Fabric resource conditions; leaving resource visible: {}", message);
        return true;
    }

    private static <T extends ResourceCondition> ResourceConditionType<T> type(String path, MapCodec<T> codec) {
        return ResourceConditionType.create(ResourceLocation.fromNamespaceAndPath("fabric", path), codec);
    }

    private record TrueCondition() implements ResourceCondition {
        private static final MapCodec<TrueCondition> CODEC = MapCodec.unit(TrueCondition::new);
        @Override public ResourceConditionType<?> getType() { return TRUE; }
        @Override public boolean test(HolderLookup.Provider lookup) { return true; }
    }

    private record NotCondition(ResourceCondition value) implements ResourceCondition {
        private static final MapCodec<NotCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceCondition.CODEC.fieldOf("value").forGetter(NotCondition::value)
        ).apply(instance, NotCondition::new));
        @Override public ResourceConditionType<?> getType() { return NOT; }
        @Override public boolean test(HolderLookup.Provider lookup) { return !value.test(lookup); }
    }

    private record AndCondition(List<ResourceCondition> values) implements ResourceCondition {
        private static final MapCodec<AndCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceCondition.CODEC.listOf().fieldOf("values").forGetter(AndCondition::values)
        ).apply(instance, AndCondition::new));
        @Override public ResourceConditionType<?> getType() { return AND; }
        @Override public boolean test(HolderLookup.Provider lookup) {
            return values.stream().allMatch(condition -> condition.test(lookup));
        }
    }

    private record OrCondition(List<ResourceCondition> values) implements ResourceCondition {
        private static final MapCodec<OrCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceCondition.CODEC.listOf().fieldOf("values").forGetter(OrCondition::values)
        ).apply(instance, OrCondition::new));
        @Override public ResourceConditionType<?> getType() { return OR; }
        @Override public boolean test(HolderLookup.Provider lookup) {
            return values.stream().anyMatch(condition -> condition.test(lookup));
        }
    }

    private record ModsCondition(List<String> values, boolean all) implements ResourceCondition {
        private static final MapCodec<ModsCondition> ALL_CODEC = codec(true);
        private static final MapCodec<ModsCondition> ANY_CODEC = codec(false);
        private static MapCodec<ModsCondition> codec(boolean all) {
            return RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.listOf().fieldOf("values").forGetter(ModsCondition::values)
            ).apply(instance, values -> new ModsCondition(values, all)));
        }
        @Override public ResourceConditionType<?> getType() { return all ? ALL_MODS : ANY_MODS; }
        @Override public boolean test(HolderLookup.Provider lookup) {
            return all
                    ? values.stream().allMatch(FabricLoader.getInstance()::isModLoaded)
                    : values.stream().anyMatch(FabricLoader.getInstance()::isModLoaded);
        }
    }

    private record TagsCondition(ResourceLocation registry, List<ResourceLocation> values) implements ResourceCondition {
        private static final MapCodec<TagsCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("registry").orElse(Registries.ITEM.location()).forGetter(TagsCondition::registry),
                ResourceLocation.CODEC.listOf().fieldOf("values").forGetter(TagsCondition::values)
        ).apply(instance, TagsCondition::new));
        @Override public ResourceConditionType<?> getType() { return TAGS; }
        @Override public boolean test(HolderLookup.Provider lookup) {
            Map<ResourceKey<?>, Set<ResourceLocation>> loaded = LOADED_TAGS.get();
            if (loaded == null) {
                LOGGER.warn("LB-FAPI-RC-002: loaded tags unavailable for tags_populated condition on {}", registry);
                return false;
            }
            Set<ResourceLocation> registryTags = loaded.get(ResourceKey.createRegistryKey(registry));
            return registryTags == null ? values.isEmpty() : registryTags.containsAll(values);
        }
    }

    private record FeaturesCondition(Collection<ResourceLocation> features) implements ResourceCondition {
        private static final MapCodec<FeaturesCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.listOf().fieldOf("features").forGetter(value -> List.copyOf(value.features))
        ).apply(instance, FeaturesCondition::new));
        @Override public ResourceConditionType<?> getType() { return FEATURES; }
        @Override public boolean test(HolderLookup.Provider lookup) {
            AtomicBoolean unknown = new AtomicBoolean();
            var requested = FeatureFlags.REGISTRY.fromNames(features, id -> unknown.set(true));
            return !unknown.get() && requested.isSubsetOf(currentFeatures);
        }
    }

    private record RegistryCondition(ResourceLocation registry, List<ResourceLocation> values) implements ResourceCondition {
        private static final MapCodec<RegistryCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("registry").orElse(Registries.ITEM.location()).forGetter(RegistryCondition::registry),
                ResourceLocation.CODEC.listOf().fieldOf("values").forGetter(RegistryCondition::values)
        ).apply(instance, RegistryCondition::new));
        @Override public ResourceConditionType<?> getType() { return REGISTRY; }
        @Override public boolean test(HolderLookup.Provider lookup) {
            if (lookup == null) {
                LOGGER.warn("LB-FAPI-RC-003: registry lookup unavailable for registry_contains condition on {}", registry);
                return false;
            }
            ResourceKey<? extends Registry<Object>> key = ResourceKey.createRegistryKey(registry);
            return lookup.lookup(key)
                    .map(registryLookup -> values.stream()
                            .allMatch(id -> registryLookup.get(ResourceKey.create(key, id)).isPresent()))
                    .orElse(values.isEmpty());
        }
    }
}
