package dev.loaderbridge.fabric.api.biome;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.util.Comparator;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
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
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class BridgeBiomeRules implements BiomeModifier {
    public static final MapCodec<BridgeBiomeRules> CODEC = new RulesCodec();
    private static final List<GenericRule> GENERIC_RULES = new CopyOnWriteArrayList<>();
    private static final AtomicLong NEXT_RULE_SEQUENCE = new AtomicLong();
    private final List<GenericRule> genericRules;
    private final HolderLookup.RegistryLookup<ConfiguredFeature<?, ?>> configuredFeatures;
    private final HolderLookup.RegistryLookup<PlacedFeature> placedFeatures;
    private final HolderGetter<ConfiguredWorldCarver<?>> configuredCarvers;
    private final HolderLookup.RegistryLookup<Structure> structures;
    private Map<ConfiguredFeature<?, ?>, ResourceKey<ConfiguredFeature<?, ?>>> configuredFeatureKeys = Map.of();
    private Map<PlacedFeature, ResourceKey<PlacedFeature>> placedFeatureKeys = Map.of();
    private Map<Structure, ResourceKey<Structure>> structureKeys = Map.of();
    private boolean registryKeysInitialized;

    private BridgeBiomeRules(List<GenericRule> genericRules,
            HolderLookup.RegistryLookup<ConfiguredFeature<?, ?>> configuredFeatures,
            HolderLookup.RegistryLookup<PlacedFeature> placedFeatures,
            HolderGetter<ConfiguredWorldCarver<?>> configuredCarvers,
            HolderLookup.RegistryLookup<Structure> structures) {
        this.genericRules = List.copyOf(genericRules);
        this.configuredFeatures = configuredFeatures;
        this.placedFeatures = placedFeatures;
        this.configuredCarvers = configuredCarvers;
        this.structures = structures;
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
        ensureRegistryKeys();
        BiomeSelectionContext context = new ForgeSelectionContext(biome, builder,
                configuredFeatureKeys, placedFeatureKeys, structures, structureKeys);
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

    private synchronized void ensureRegistryKeys() {
        if (registryKeysInitialized) return;
        // Forge may rebind dynamic-registry holder values between codec decode and
        // server modifier application. Index the live, post-freeze identities here.
        configuredFeatureKeys = registryKeys(configuredFeatures);
        placedFeatureKeys = registryKeys(placedFeatures);
        structureKeys = registryKeys(structures);
        registryKeysInitialized = true;
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
            Optional<HolderLookup.RegistryLookup<ConfiguredFeature<?, ?>>> configured =
                    registryLookup(registryOps, Registries.CONFIGURED_FEATURE);
            if (configured.isEmpty()) {
                return DataResult.error(() ->
                        "LB-BIOME-001: configured-feature registry is unavailable");
            }
            Optional<HolderLookup.RegistryLookup<PlacedFeature>> placed =
                    registryLookup(registryOps, Registries.PLACED_FEATURE);
            if (placed.isEmpty()) {
                return DataResult.error(() -> "LB-BIOME-001: placed-feature registry is unavailable");
            }
            Optional<HolderGetter<ConfiguredWorldCarver<?>>> carverGetter =
                    configuredCarverGetter(registryOps);
            if (carverGetter.isEmpty()) {
                return DataResult.error(() ->
                        "LB-BIOME-001: configured-carver registry is unavailable");
            }
            Optional<HolderLookup.RegistryLookup<Structure>> structures =
                    registryLookup(registryOps, Registries.STRUCTURE);
            if (structures.isEmpty()) {
                return DataResult.error(() -> "LB-BIOME-001: structure registry is unavailable");
            }
            List<GenericRule> generic = GENERIC_RULES.stream()
                    .sorted(Comparator.comparing((GenericRule rule) -> rule.phase().ordinal())
                            .thenComparing(rule -> rule.id().toString())
                            .thenComparingLong(GenericRule::sequence))
                    .toList();
            return DataResult.success(new BridgeBiomeRules(
                    generic, configured.get(), placed.get(), carverGetter.get(),
                    structures.get()));
        }

        @SuppressWarnings("unchecked")
        private static <T> Optional<HolderLookup.RegistryLookup<T>> registryLookup(
                RegistryOps<?> ops, ResourceKey<? extends Registry<? extends T>> registryKey) {
            return ops.owner(registryKey).flatMap(owner ->
                    owner instanceof HolderLookup.RegistryLookup<?> lookup
                            ? Optional.of((HolderLookup.RegistryLookup<T>) lookup)
                            : Optional.empty());
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
        private final Map<ConfiguredFeature<?, ?>, ResourceKey<ConfiguredFeature<?, ?>>> configuredFeatureKeys;
        private final Map<PlacedFeature, ResourceKey<PlacedFeature>> placedFeatureKeys;
        private final HolderLookup.RegistryLookup<Structure> structures;
        private final Map<Structure, ResourceKey<Structure>> structureKeys;

        ForgeSelectionContext(Holder<Biome> biome, ModifiableBiomeInfo.BiomeInfo.Builder builder,
                Map<ConfiguredFeature<?, ?>, ResourceKey<ConfiguredFeature<?, ?>>> configuredFeatureKeys,
                Map<PlacedFeature, ResourceKey<PlacedFeature>> placedFeatureKeys,
                HolderLookup.RegistryLookup<Structure> structures,
                Map<Structure, ResourceKey<Structure>> structureKeys) {
            this.biome = biome;
            this.builder = builder;
            this.configuredFeatureKeys = configuredFeatureKeys;
            this.placedFeatureKeys = placedFeatureKeys;
            this.structures = structures;
            this.structureKeys = structureKeys;
        }
        @Override public ResourceKey<Biome> getBiomeKey() { return biome.unwrapKey().orElseThrow(); }
        @Override public Biome getBiome() {
            ModifiableBiomeInfo.BiomeInfo info = builder.build();
            return BiomeInvoker.loaderbridge$create(info.climateSettings(), info.effects(),
                    info.generationSettings(), info.mobSpawnSettings());
        }
        @Override public Holder<Biome> getBiomeRegistryEntry() { return biome; }
        @Override public Optional<ResourceKey<ConfiguredFeature<?, ?>>> getFeatureKey(
                ConfiguredFeature<?, ?> feature) {
            return Optional.ofNullable(configuredFeatureKeys.get(feature));
        }
        @Override public Optional<ResourceKey<PlacedFeature>> getPlacedFeatureKey(PlacedFeature feature) {
            return Optional.ofNullable(placedFeatureKeys.get(feature));
        }
        @Override public boolean validForStructure(ResourceKey<Structure> structureKey) {
            return structures.get(structureKey)
                    .map(structure -> structure.value().biomes().contains(biome))
                    .orElse(false);
        }
        @Override public Optional<ResourceKey<Structure>> getStructureKey(Structure structure) {
            return Optional.ofNullable(structureKeys.get(structure));
        }
        @Override public boolean canGenerateIn(ResourceKey<LevelStem> dimensionKey) {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return false;
            return server.registryAccess().registry(Registries.LEVEL_STEM)
                    .flatMap(registry -> registry.getHolder(dimensionKey))
                    .map(dimension -> dimension.value().generator().getBiomeSource()
                            .possibleBiomes().stream().anyMatch(candidate ->
                                    candidate.unwrapKey().equals(biome.unwrapKey())))
                    .orElse(false);
        }
        @Override public boolean hasTag(TagKey<Biome> tag) { return biome.is(tag); }
    }

    static <T> Map<T, ResourceKey<T>> registryKeys(HolderLookup<T> registry) {
        Map<T, ResourceKey<T>> keys = new IdentityHashMap<>();
        registry.listElements().forEach(holder ->
                holder.unwrapKey().ifPresent(key -> keys.put(holder.value(), key)));
        return Collections.unmodifiableMap(keys);
    }
}
