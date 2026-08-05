package net.fabricmc.fabric.api.biome.v1;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BiPredicate;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.AmbientAdditionsSettings;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public interface BiomeModificationContext {
    WeatherContext getWeather();
    EffectsContext getEffects();
    GenerationSettingsContext getGenerationSettings();
    SpawnSettingsContext getSpawnSettings();

    interface WeatherContext {
        void setPrecipitation(boolean hasPrecipitation);
        void setTemperature(float temperature);
        void setTemperatureModifier(Biome.TemperatureModifier temperatureModifier);
        void setDownfall(float downfall);
    }

    interface EffectsContext {
        void setFogColor(int color);
        void setWaterColor(int color);
        void setWaterFogColor(int color);
        void setSkyColor(int color);
        void setFoliageColor(Optional<Integer> color);
        default void setFoliageColor(int color) { setFoliageColor(Optional.of(color)); }
        default void setFoliageColor(OptionalInt color) {
            color.ifPresentOrElse(this::setFoliageColor, this::clearFoliageColor);
        }
        default void clearFoliageColor() { setFoliageColor(Optional.empty()); }
        void setGrassColor(Optional<Integer> color);
        default void setGrassColor(int color) { setGrassColor(Optional.of(color)); }
        default void setGrassColor(OptionalInt color) {
            color.ifPresentOrElse(this::setGrassColor, this::clearGrassColor);
        }
        default void clearGrassColor() { setGrassColor(Optional.empty()); }
        void setGrassColorModifier(BiomeSpecialEffects.GrassColorModifier colorModifier);
        void setParticleConfig(Optional<AmbientParticleSettings> particleConfig);
        default void setParticleConfig(AmbientParticleSettings particleConfig) {
            setParticleConfig(Optional.of(particleConfig));
        }
        default void clearParticleConfig() { setParticleConfig(Optional.empty()); }
        void setAmbientSound(Optional<Holder<SoundEvent>> sound);
        default void setAmbientSound(Holder<SoundEvent> sound) { setAmbientSound(Optional.of(sound)); }
        default void clearAmbientSound() { setAmbientSound(Optional.empty()); }
        void setMoodSound(Optional<AmbientMoodSettings> sound);
        default void setMoodSound(AmbientMoodSettings sound) { setMoodSound(Optional.of(sound)); }
        default void clearMoodSound() { setMoodSound(Optional.empty()); }
        void setAdditionsSound(Optional<AmbientAdditionsSettings> sound);
        default void setAdditionsSound(AmbientAdditionsSettings sound) {
            setAdditionsSound(Optional.of(sound));
        }
        default void clearAdditionsSound() { setAdditionsSound(Optional.empty()); }
        void setMusic(Optional<Music> sound);
        default void setMusic(Music sound) { setMusic(Optional.of(sound)); }
        default void clearMusic() { setMusic(Optional.empty()); }
    }

    interface GenerationSettingsContext {
        boolean removeFeature(GenerationStep.Decoration step, ResourceKey<PlacedFeature> placedFeatureKey);
        default boolean removeFeature(ResourceKey<PlacedFeature> placedFeatureKey) {
            boolean removed = false;
            for (GenerationStep.Decoration step : GenerationStep.Decoration.values()) {
                removed |= removeFeature(step, placedFeatureKey);
            }
            return removed;
        }
        void addFeature(GenerationStep.Decoration step, ResourceKey<PlacedFeature> placedFeatureKey);
        void addCarver(GenerationStep.Carving step,
                ResourceKey<ConfiguredWorldCarver<?>> configuredCarverKey);
        boolean removeCarver(GenerationStep.Carving step,
                ResourceKey<ConfiguredWorldCarver<?>> configuredCarverKey);
        default boolean removeCarver(ResourceKey<ConfiguredWorldCarver<?>> configuredCarverKey) {
            boolean removed = false;
            for (GenerationStep.Carving step : GenerationStep.Carving.values()) {
                removed |= removeCarver(step, configuredCarverKey);
            }
            return removed;
        }
    }

    interface SpawnSettingsContext {
        void setCreatureSpawnProbability(float probability);
        void addSpawn(MobCategory spawnGroup, MobSpawnSettings.SpawnerData spawnEntry);
        boolean removeSpawns(BiPredicate<MobCategory, MobSpawnSettings.SpawnerData> predicate);
        default boolean removeSpawnsOfEntityType(EntityType<?> entityType) {
            return removeSpawns((group, entry) -> entry.type == entityType);
        }
        default void clearSpawns(MobCategory group) {
            removeSpawns((candidate, entry) -> candidate == group);
        }
        default void clearSpawns() { removeSpawns((group, entry) -> true); }
        void setSpawnCost(EntityType<?> entityType, double mass, double gravityLimit);
        void clearSpawnCost(EntityType<?> entityType);
    }
}
