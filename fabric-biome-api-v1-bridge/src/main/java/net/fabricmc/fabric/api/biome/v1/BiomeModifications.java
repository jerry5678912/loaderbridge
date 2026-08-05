package net.fabricmc.fabric.api.biome.v1;

import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class BiomeModifications {
    private BiomeModifications() { }

    public static void addFeature(Predicate<BiomeSelectionContext> selector,
            GenerationStep.Decoration step, ResourceKey<PlacedFeature> featureKey) {
        create(featureKey.location()).add(ModificationPhase.ADDITIONS, selector,
                context -> context.getGenerationSettings().addFeature(step, featureKey));
    }

    public static void addCarver(Predicate<BiomeSelectionContext> selector,
            GenerationStep.Carving step,
            ResourceKey<ConfiguredWorldCarver<?>> configuredCarverKey) {
        create(configuredCarverKey.location()).add(ModificationPhase.ADDITIONS, selector,
                context -> context.getGenerationSettings().addCarver(step, configuredCarverKey));
    }

    public static void addSpawn(Predicate<BiomeSelectionContext> selector,
            MobCategory category, EntityType<?> entityType, int weight,
            int minimumGroupSize, int maximumGroupSize) {
        if (entityType.getCategory() == MobCategory.MISC) {
            throw new IllegalArgumentException(
                    "Cannot add spawns for entities with spawnGroup=MISC since they'd be replaced by pigs.");
        }
        if (BuiltInRegistries.ENTITY_TYPE.getResourceKey(entityType).isEmpty()) {
            throw new IllegalStateException("Unregistered entity type: " + entityType);
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        create(id).add(ModificationPhase.ADDITIONS, selector,
                context -> context.getSpawnSettings().addSpawn(category,
                        new MobSpawnSettings.SpawnerData(
                                entityType, weight, minimumGroupSize, maximumGroupSize)));
    }

    public static BiomeModification create(ResourceLocation id) {
        return new BiomeModification(id);
    }
}
