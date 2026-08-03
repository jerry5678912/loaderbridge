package dev.loaderbridge.fabric.api.biome;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

public final class BridgeBiomeRules implements BiomeModifier {
    public static final MapCodec<BridgeBiomeRules> CODEC = new RulesCodec();
    private static final List<FeatureRule> FEATURE_RULES = new CopyOnWriteArrayList<>();
    private final List<ResolvedFeatureRule> features;

    private BridgeBiomeRules(List<ResolvedFeatureRule> features) {
        this.features = List.copyOf(features);
    }

    public static void addFeature(Predicate<BiomeSelectionContext> selector,
            GenerationStep.Decoration step, ResourceKey<PlacedFeature> featureKey) {
        FEATURE_RULES.add(new FeatureRule(selector, step, featureKey));
    }

    static int featureRuleCount() { return FEATURE_RULES.size(); }

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD) return;
        BiomeSelectionContext context = new ForgeSelectionContext(biome);
        for (ResolvedFeatureRule rule : features) {
            if (rule.selector().test(context)) {
                builder.getGenerationSettings().addFeature(rule.step(), rule.feature());
            }
        }
    }

    @Override public MapCodec<? extends BiomeModifier> codec() { return CODEC; }

    private record FeatureRule(Predicate<BiomeSelectionContext> selector,
                               GenerationStep.Decoration step,
                               ResourceKey<PlacedFeature> featureKey) { }
    private record ResolvedFeatureRule(Predicate<BiomeSelectionContext> selector,
                                       GenerationStep.Decoration step,
                                       Holder<PlacedFeature> feature) { }

    private static final class RulesCodec extends MapCodec<BridgeBiomeRules> {
        @Override
        public <T> DataResult<BridgeBiomeRules> decode(DynamicOps<T> ops, MapLike<T> input) {
            if (!(ops instanceof RegistryOps<?> registryOps)) {
                return DataResult.error(() -> "LB-BIOME-001: registry-aware operations are required");
            }
            Optional<HolderGetter<PlacedFeature>> getter = placedFeatureGetter(registryOps);
            if (getter.isEmpty()) {
                return DataResult.error(() -> "LB-BIOME-001: placed-feature registry is unavailable");
            }
            List<ResolvedFeatureRule> resolved = new ArrayList<>();
            for (FeatureRule rule : FEATURE_RULES) {
                Optional<? extends Holder<PlacedFeature>> feature = getter.get().get(rule.featureKey());
                if (feature.isEmpty()) {
                    return DataResult.error(() -> "LB-BIOME-002: missing placed feature "
                            + rule.featureKey().location());
                }
                resolved.add(new ResolvedFeatureRule(rule.selector(), rule.step(), feature.get()));
            }
            return DataResult.success(new BridgeBiomeRules(resolved));
        }

        @SuppressWarnings("unchecked")
        private static Optional<HolderGetter<PlacedFeature>> placedFeatureGetter(RegistryOps<?> ops) {
            return (Optional<HolderGetter<PlacedFeature>>) (Optional<?>) ops.getter(Registries.PLACED_FEATURE);
        }

        @Override
        public <T> RecordBuilder<T> encode(BridgeBiomeRules input, DynamicOps<T> ops,
                RecordBuilder<T> prefix) {
            return prefix;
        }

        @Override public <T> Stream<T> keys(DynamicOps<T> ops) { return Stream.empty(); }
    }

    private static final class ForgeSelectionContext implements BiomeSelectionContext {
        private final Holder<Biome> biome;

        ForgeSelectionContext(Holder<Biome> biome) { this.biome = biome; }
        @Override public ResourceKey<Biome> getBiomeKey() { return biome.unwrapKey().orElseThrow(); }
        @Override public Biome getBiome() { return biome.value(); }
        @Override public Holder<Biome> getBiomeRegistryEntry() { return biome; }
        @Override public Optional<ResourceKey<ConfiguredFeature<?, ?>>> getFeatureKey(
                ConfiguredFeature<?, ?> feature) { return Optional.empty(); }
        @Override public Optional<ResourceKey<PlacedFeature>> getPlacedFeatureKey(PlacedFeature feature) {
            return getBiome().getGenerationSettings().features().stream().flatMap(holders -> holders.stream())
                    .filter(holder -> holder.value() == feature).findFirst().flatMap(Holder::unwrapKey);
        }
        @Override public boolean validForStructure(ResourceKey<Structure> structureKey) { return false; }
        @Override public Optional<ResourceKey<Structure>> getStructureKey(Structure structure) {
            return Optional.empty();
        }
        @Override public boolean canGenerateIn(ResourceKey<LevelStem> dimensionKey) {
            if (LevelStem.OVERWORLD.equals(dimensionKey)) return biome.is(BiomeTags.IS_OVERWORLD);
            if (LevelStem.NETHER.equals(dimensionKey)) return biome.is(BiomeTags.IS_NETHER);
            if (LevelStem.END.equals(dimensionKey)) return biome.is(BiomeTags.IS_END);
            return false;
        }
        @Override public boolean hasTag(TagKey<Biome> tag) { return biome.is(tag); }
    }
}
