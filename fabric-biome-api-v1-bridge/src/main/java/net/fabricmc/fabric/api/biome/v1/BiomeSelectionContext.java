package net.fabricmc.fabric.api.biome.v1;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;

public interface BiomeSelectionContext {
    ResourceKey<Biome> getBiomeKey();
    Biome getBiome();
    Holder<Biome> getBiomeRegistryEntry();

    default boolean hasFeature(ResourceKey<ConfiguredFeature<?, ?>> featureKey) {
        return getBiome().getGenerationSettings().features().stream().flatMap(holders -> holders.stream())
                .flatMap(holder -> holder.value().getFeatures())
                .anyMatch(feature -> getFeatureKey(feature).filter(featureKey::equals).isPresent());
    }

    default boolean hasPlacedFeature(ResourceKey<PlacedFeature> featureKey) {
        return getBiome().getGenerationSettings().features().stream().flatMap(holders -> holders.stream())
                .anyMatch(holder -> holder.is(featureKey));
    }

    Optional<ResourceKey<ConfiguredFeature<?, ?>>> getFeatureKey(ConfiguredFeature<?, ?> feature);
    Optional<ResourceKey<PlacedFeature>> getPlacedFeatureKey(PlacedFeature feature);
    boolean validForStructure(ResourceKey<Structure> structureKey);
    Optional<ResourceKey<Structure>> getStructureKey(Structure structure);
    boolean canGenerateIn(ResourceKey<LevelStem> dimensionKey);
    boolean hasTag(TagKey<Biome> tag);
}
