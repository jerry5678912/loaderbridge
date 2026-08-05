package dev.loaderbridge.fabric.api.biome;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;

public final class NetherBiomeEntries {
    private static final Map<ResourceKey<Biome>, Climate.ParameterPoint> ADDITIONS =
            new LinkedHashMap<>();

    private NetherBiomeEntries() { }

    public static synchronized void add(ResourceKey<Biome> biome,
            Climate.ParameterPoint noisePoint) {
        ADDITIONS.put(Objects.requireNonNull(biome, "Biome is null"),
                Objects.requireNonNull(noisePoint, "MultiNoiseUtil.NoiseValuePoint is null"));
    }

    public static synchronized boolean canGenerate(ResourceKey<Biome> biome) {
        if (ADDITIONS.containsKey(biome)) return true;
        return MultiNoiseBiomeSourceParameterList.Preset.NETHER.usedBiomes()
                .anyMatch(biome::equals);
    }

    public static synchronized <T> Climate.ParameterList<T> withModdedEntries(
            Climate.ParameterList<T> entries, Function<ResourceKey<Biome>, T> biomes) {
        if (ADDITIONS.isEmpty()) return entries;
        List<Pair<Climate.ParameterPoint, T>> values = new ArrayList<>(entries.values());
        ADDITIONS.forEach((biome, point) -> values.add(Pair.of(point, biomes.apply(biome))));
        return new Climate.ParameterList<>(List.copyOf(values));
    }
}
