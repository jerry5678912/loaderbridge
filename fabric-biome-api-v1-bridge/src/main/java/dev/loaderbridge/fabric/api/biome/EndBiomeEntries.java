package dev.loaderbridge.fabric.api.biome;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

public final class EndBiomeEntries {
    private static final Map<ResourceKey<Biome>, WeightedPicker<ResourceKey<Biome>>> REPLACEMENTS =
            new LinkedHashMap<>();
    private static final Map<ResourceKey<Biome>, WeightedPicker<ResourceKey<Biome>>> MIDLANDS =
            new LinkedHashMap<>();
    private static final Map<ResourceKey<Biome>, WeightedPicker<ResourceKey<Biome>>> BARRENS =
            new LinkedHashMap<>();
    private static final Set<ResourceKey<Biome>> ADDED_BIOMES = new LinkedHashSet<>();
    private static final ThreadLocal<HolderGetter<Biome>> CONSTRUCTION_LOOKUP = new ThreadLocal<>();

    static {
        picker(REPLACEMENTS, Biomes.THE_END).add(Biomes.THE_END, 1.0);
        picker(REPLACEMENTS, Biomes.END_HIGHLANDS).add(Biomes.END_HIGHLANDS, 1.0);
        picker(REPLACEMENTS, Biomes.SMALL_END_ISLANDS).add(Biomes.SMALL_END_ISLANDS, 1.0);
        picker(MIDLANDS, Biomes.END_HIGHLANDS).add(Biomes.END_MIDLANDS, 1.0);
        picker(BARRENS, Biomes.END_HIGHLANDS).add(Biomes.END_BARRENS, 1.0);
    }

    private EndBiomeEntries() { }

    public static synchronized void addReplacement(ResourceKey<Biome> replaced,
            ResourceKey<Biome> replacement, double weight) {
        validate(replaced, "replaced entry is null");
        validate(replacement, "variant entry is null");
        validateWeight(weight);
        picker(REPLACEMENTS, replaced).add(replacement, weight);
        ADDED_BIOMES.add(replacement);
    }

    public static synchronized void addMidlands(ResourceKey<Biome> highlands,
            ResourceKey<Biome> midlands, double weight) {
        validate(highlands, "highlands entry is null");
        validate(midlands, "midlands entry is null");
        validateWeight(weight);
        picker(MIDLANDS, highlands).add(midlands, weight);
        ADDED_BIOMES.add(midlands);
    }

    public static synchronized void addBarrens(ResourceKey<Biome> highlands,
            ResourceKey<Biome> barrens, double weight) {
        validate(highlands, "highlands entry is null");
        validate(barrens, "midlands entry is null");
        validateWeight(weight);
        picker(BARRENS, highlands).add(barrens, weight);
        ADDED_BIOMES.add(barrens);
    }

    public static void rememberLookup(HolderGetter<Biome> lookup) {
        CONSTRUCTION_LOOKUP.set(lookup);
    }

    public static void clearLookup() {
        CONSTRUCTION_LOOKUP.remove();
    }

    public static Overrides createOverrides() {
        HolderGetter<Biome> lookup = CONSTRUCTION_LOOKUP.get();
        if (lookup == null) {
            throw new IllegalStateException("Biome registry not set by LoaderBridge Mixin");
        }
        return new Overrides(lookup);
    }

    private static <T> T validate(T value, String message) {
        return Objects.requireNonNull(value, message);
    }

    private static void validateWeight(double weight) {
        if (!(weight > 0.0)) {
            throw new IllegalArgumentException(
                    "Weight is less than or equal to 0.0 (got " + weight + ")");
        }
    }

    private static <K, V> WeightedPicker<V> picker(Map<K, WeightedPicker<V>> map, K key) {
        return map.computeIfAbsent(key, ignored -> new WeightedPicker<>());
    }

    public static final class Overrides {
        private final Set<Holder<Biome>> customBiomes;
        private final Holder<Biome> endHighlands;
        private final Holder<Biome> endMidlands;
        private final Holder<Biome> endBarrens;
        private final Map<Holder<Biome>, WeightedPicker<Holder<Biome>>> replacements;
        private final Map<Holder<Biome>, WeightedPicker<Holder<Biome>>> midlands;
        private final Map<Holder<Biome>, WeightedPicker<Holder<Biome>>> barrens;

        private Overrides(HolderGetter<Biome> lookup) {
            synchronized (EndBiomeEntries.class) {
                customBiomes = resolveSet(lookup, ADDED_BIOMES);
                endHighlands = lookup.getOrThrow(Biomes.END_HIGHLANDS);
                endMidlands = lookup.getOrThrow(Biomes.END_MIDLANDS);
                endBarrens = lookup.getOrThrow(Biomes.END_BARRENS);
                replacements = resolveMap(lookup, REPLACEMENTS, Biomes.THE_END);
                midlands = resolveMap(lookup, MIDLANDS, Biomes.END_MIDLANDS);
                barrens = resolveMap(lookup, BARRENS, Biomes.END_BARRENS);
            }
        }

        public Set<Holder<Biome>> customBiomes() {
            return customBiomes;
        }

        public Holder<Biome> pick(int x, int z, Climate.Sampler sampler,
                Holder<Biome> vanillaBiome) {
            boolean isMidlands = vanillaBiome.is(endMidlands.unwrapKey().orElseThrow());
            boolean isBarrens = vanillaBiome.is(endBarrens.unwrapKey().orElseThrow());
            if (isMidlands || isBarrens) {
                Holder<Biome> highlands = pick(endHighlands, endHighlands, replacements,
                        x, z, sampler);
                return pick(highlands, vanillaBiome, isMidlands ? midlands : barrens,
                        x, z, sampler);
            }
            return pick(vanillaBiome, vanillaBiome, replacements, x, z, sampler);
        }

        private Holder<Biome> pick(Holder<Biome> key, Holder<Biome> fallback,
                Map<Holder<Biome>, WeightedPicker<Holder<Biome>>> pickers,
                int x, int z, Climate.Sampler sampler) {
            WeightedPicker<Holder<Biome>> picker = pickers.get(key);
            if (picker == null || picker.size() == 0
                    || (picker.size() == 1 && key.is(Biomes.END_HIGHLANDS))) {
                return fallback;
            }
            var noise = ((EndClimateSampler) (Object) sampler).loaderbridge$getEndBiomeNoise();
            return picker.pick(noise, x / 64.0, z / 64.0);
        }

        private static Set<Holder<Biome>> resolveSet(HolderGetter<Biome> lookup,
                Set<ResourceKey<Biome>> keys) {
            Set<Holder<Biome>> result = new LinkedHashSet<>();
            keys.forEach(key -> result.add(lookup.getOrThrow(key)));
            return Set.copyOf(result);
        }

        private static Map<Holder<Biome>, WeightedPicker<Holder<Biome>>> resolveMap(
                HolderGetter<Biome> lookup,
                Map<ResourceKey<Biome>, WeightedPicker<ResourceKey<Biome>>> source,
                ResourceKey<Biome> noOpVanillaKey) {
            Map<Holder<Biome>, WeightedPicker<Holder<Biome>>> result = new LinkedHashMap<>();
            source.forEach((key, picker) -> {
                if (picker.size() == 0 || (picker.size() == 1 && key == noOpVanillaKey)) return;
                result.put(lookup.getOrThrow(key), picker.map(lookup::getOrThrow));
            });
            return Map.copyOf(result);
        }
    }
}
