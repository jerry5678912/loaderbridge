package dev.loaderbridge.fabric.api.biome;

import java.util.Optional;
import java.util.function.BiPredicate;
import net.fabricmc.fabric.api.biome.v1.BiomeModificationContext;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
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
import net.minecraftforge.common.world.BiomeGenerationSettingsBuilder;
import net.minecraftforge.common.world.BiomeSpecialEffectsBuilder;
import net.minecraftforge.common.world.ClimateSettingsBuilder;
import net.minecraftforge.common.world.MobSpawnSettingsBuilder;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

final class ForgeModificationContext implements BiomeModificationContext {
    private final WeatherContext weather;
    private final EffectsContext effects;
    private final GenerationSettingsContext generation;
    private final SpawnSettingsContext spawns;

    ForgeModificationContext(ModifiableBiomeInfo.BiomeInfo.Builder builder,
            HolderGetter<PlacedFeature> placedFeatures,
            HolderGetter<ConfiguredWorldCarver<?>> configuredCarvers) {
        weather = new Weather(builder.getClimateSettings());
        effects = new Effects(builder.getSpecialEffects());
        generation = new Generation(builder.getGenerationSettings(), placedFeatures, configuredCarvers);
        spawns = new Spawns(builder.getMobSpawnSettings());
    }

    @Override public WeatherContext getWeather() { return weather; }
    @Override public EffectsContext getEffects() { return effects; }
    @Override public GenerationSettingsContext getGenerationSettings() { return generation; }
    @Override public SpawnSettingsContext getSpawnSettings() { return spawns; }

    private record Weather(ClimateSettingsBuilder builder) implements WeatherContext {
        @Override public void setPrecipitation(boolean value) { builder.setHasPrecipitation(value); }
        @Override public void setTemperature(float value) { builder.setTemperature(value); }
        @Override public void setTemperatureModifier(Biome.TemperatureModifier value) {
            builder.setTemperatureModifier(value);
        }
        @Override public void setDownfall(float value) { builder.setDownfall(value); }
    }

    private record Effects(BiomeSpecialEffectsBuilder builder) implements EffectsContext {
        @Override public void setFogColor(int color) { builder.fogColor(color); }
        @Override public void setWaterColor(int color) { builder.waterColor(color); }
        @Override public void setWaterFogColor(int color) { builder.waterFogColor(color); }
        @Override public void setSkyColor(int color) { builder.skyColor(color); }
        @Override public void setFoliageColor(Optional<Integer> color) {
            color.ifPresentOrElse(builder::foliageColorOverride,
                    () -> unsupportedClear("foliage color"));
        }
        @Override public void setGrassColor(Optional<Integer> color) {
            color.ifPresentOrElse(builder::grassColorOverride,
                    () -> unsupportedClear("grass color"));
        }
        @Override public void setGrassColorModifier(BiomeSpecialEffects.GrassColorModifier modifier) {
            builder.grassColorModifier(modifier);
        }
        @Override public void setParticleConfig(Optional<AmbientParticleSettings> value) {
            value.ifPresentOrElse(builder::ambientParticle,
                    () -> unsupportedClear("ambient particle"));
        }
        @Override public void setAmbientSound(Optional<Holder<SoundEvent>> value) {
            value.ifPresentOrElse(builder::ambientLoopSound,
                    () -> unsupportedClear("ambient sound"));
        }
        @Override public void setMoodSound(Optional<AmbientMoodSettings> value) {
            value.ifPresentOrElse(builder::ambientMoodSound,
                    () -> unsupportedClear("mood sound"));
        }
        @Override public void setAdditionsSound(Optional<AmbientAdditionsSettings> value) {
            value.ifPresentOrElse(builder::ambientAdditionsSound,
                    () -> unsupportedClear("additions sound"));
        }
        @Override public void setMusic(Optional<Music> value) {
            value.ifPresentOrElse(builder::backgroundMusic,
                    () -> unsupportedClear("music"));
        }
        private static void unsupportedClear(String property) {
            throw new UnsupportedOperationException(
                    "LB-BIOME-004: Forge 52 does not expose safe removal for biome " + property);
        }
    }

    private record Generation(BiomeGenerationSettingsBuilder builder,
                              HolderGetter<PlacedFeature> placedFeatures,
                              HolderGetter<ConfiguredWorldCarver<?>> configuredCarvers)
            implements GenerationSettingsContext {
        @Override
        public boolean removeFeature(GenerationStep.Decoration step,
                ResourceKey<PlacedFeature> key) {
            return builder.getFeatures(step).removeIf(holder -> holder.is(key));
        }

        @Override
        public void addFeature(GenerationStep.Decoration step, ResourceKey<PlacedFeature> key) {
            builder.addFeature(step, required(placedFeatures, key, "LB-BIOME-002"));
        }

        @Override
        public void addCarver(GenerationStep.Carving step,
                ResourceKey<ConfiguredWorldCarver<?>> key) {
            builder.addCarver(step, required(configuredCarvers, key, "LB-BIOME-003"));
        }

        @Override
        public boolean removeCarver(GenerationStep.Carving step,
                ResourceKey<ConfiguredWorldCarver<?>> key) {
            return builder.getCarvers(step).removeIf(holder -> holder.is(key));
        }
    }

    private record Spawns(MobSpawnSettingsBuilder builder) implements SpawnSettingsContext {
        @Override public void setCreatureSpawnProbability(float value) {
            builder.creatureGenerationProbability(value);
        }
        @Override public void addSpawn(MobCategory group, MobSpawnSettings.SpawnerData entry) {
            builder.addSpawn(group, entry);
        }
        @Override
        public boolean removeSpawns(BiPredicate<MobCategory, MobSpawnSettings.SpawnerData> predicate) {
            boolean removed = false;
            for (MobCategory group : builder.getSpawnerTypes()) {
                removed |= builder.getSpawner(group).removeIf(entry -> predicate.test(group, entry));
            }
            return removed;
        }
        @Override public void setSpawnCost(EntityType<?> type, double mass, double gravityLimit) {
            builder.addMobCharge(type, mass, gravityLimit);
        }
        @Override public void clearSpawnCost(EntityType<?> type) {
            throw new UnsupportedOperationException(
                    "LB-BIOME-005: Forge 52 does not expose safe biome spawn-cost removal for " + type);
        }
    }

    private static <T> Holder<T> required(HolderGetter<T> getter, ResourceKey<T> key, String code) {
        return getter.get(key).orElseThrow(() -> new IllegalStateException(
                code + ": missing registry target " + key.location()));
    }
}
