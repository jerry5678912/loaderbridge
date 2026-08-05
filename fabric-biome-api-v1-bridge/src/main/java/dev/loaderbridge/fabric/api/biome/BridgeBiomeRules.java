package dev.loaderbridge.fabric.api.biome;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeModificationContext;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import dev.loaderbridge.fabric.api.biome.mixin.BiomeInvoker;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

public final class BridgeBiomeRules implements BiomeModifier {
    public static final MapCodec<BridgeBiomeRules> CODEC = new RulesCodec();
    private static final List<GenericRule> GENERIC_RULES = new CopyOnWriteArrayList<>();
    private static final AtomicLong NEXT_RULE_SEQUENCE = new AtomicLong();
    private final List<GenericRule> genericRules;
    private final HolderGetter<PlacedFeature> placedFeatures;
    private final HolderGetter<ConfiguredWorldCarver<?>> configuredCarvers;

    private BridgeBiomeRules(List<GenericRule> genericRules,
            HolderGetter<PlacedFeature> placedFeatures,
            HolderGetter<ConfiguredWorldCarver<?>> configuredCarvers) {
        this.genericRules = List.copyOf(genericRules);
        this.placedFeatures = placedFeatures;
        this.configuredCarvers = configuredCarvers;
    }

    public static void addModification(ResourceLocation id, ModificationPhase phase,
            Predicate<BiomeSelectionContext> selector,
            BiConsumer<BiomeSelectionContext, BiomeModificationContext> modifier) {
        GENERIC_RULES.add(new GenericRule(id, phase, selector, modifier,
                NEXT_RULE_SEQUENCE.getAndIncrement()));
    }

    static int genericRuleCount() { return GENERIC_RULES.size(); }

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        BiomeSelectionContext context = new ForgeSelectionContext(biome, builder);
        ModificationPhase fabricPhase = fabricPhase(phase);
        if (fabricPhase == null) return;
        ForgeModificationContext modificationContext =
                new ForgeModificationContext(builder, placedFeatures, configuredCarvers);
        for (GenericRule rule : genericRules) {
            if (rule.phase() == fabricPhase && rule.selector().test(context)) {
                rule.modifier().accept(context, modificationContext);
            }
        }
    }

    @Override public MapCodec<? extends BiomeModifier> codec() { return CODEC; }

    private record GenericRule(ResourceLocation id, ModificationPhase phase,
                               Predicate<BiomeSelectionContext> selector,
                               BiConsumer<BiomeSelectionContext, BiomeModificationContext> modifier,
                               long sequence) { }

    private static ModificationPhase fabricPhase(Phase phase) {
        return switch (phase) {
            case ADD -> ModificationPhase.ADDITIONS;
            case REMOVE -> ModificationPhase.REMOVALS;
            case MODIFY -> ModificationPhase.REPLACEMENTS;
            case AFTER_EVERYTHING -> ModificationPhase.POST_PROCESSING;
            default -> null;
        };
    }

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
            Optional<HolderGetter<ConfiguredWorldCarver<?>>> carverGetter =
                    configuredCarverGetter(registryOps);
            if (carverGetter.isEmpty()) {
                return DataResult.error(() ->
                        "LB-BIOME-001: configured-carver registry is unavailable");
            }
            List<GenericRule> generic = GENERIC_RULES.stream()
                    .sorted(Comparator.comparing((GenericRule rule) -> rule.phase().ordinal())
                            .thenComparing(rule -> rule.id().toString())
                            .thenComparingLong(GenericRule::sequence))
                    .toList();
            return DataResult.success(new BridgeBiomeRules(
                    generic, getter.get(), carverGetter.get()));
        }

        @SuppressWarnings("unchecked")
        private static Optional<HolderGetter<PlacedFeature>> placedFeatureGetter(RegistryOps<?> ops) {
            return (Optional<HolderGetter<PlacedFeature>>) (Optional<?>) ops.getter(Registries.PLACED_FEATURE);
        }

        @SuppressWarnings("unchecked")
        private static Optional<HolderGetter<ConfiguredWorldCarver<?>>> configuredCarverGetter(
                RegistryOps<?> ops) {
            return (Optional<HolderGetter<ConfiguredWorldCarver<?>>>) (Optional<?>)
                    ops.getter(Registries.CONFIGURED_CARVER);
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
        private final ModifiableBiomeInfo.BiomeInfo.Builder builder;

        ForgeSelectionContext(Holder<Biome> biome, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
            this.biome = biome;
            this.builder = builder;
        }
        @Override public ResourceKey<Biome> getBiomeKey() { return biome.unwrapKey().orElseThrow(); }
        @Override public Biome getBiome() {
            ModifiableBiomeInfo.BiomeInfo info = builder.build();
            return BiomeInvoker.loaderbridge$create(info.climateSettings(), info.effects(),
                    info.generationSettings(), info.mobSpawnSettings());
        }
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
