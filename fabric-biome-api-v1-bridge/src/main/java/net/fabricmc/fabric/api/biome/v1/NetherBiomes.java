package net.fabricmc.fabric.api.biome.v1;

import dev.loaderbridge.fabric.api.biome.NetherBiomeEntries;
import java.util.Objects;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;

public final class NetherBiomes {
    private NetherBiomes() { }

    public static void addNetherBiome(ResourceKey<Biome> biome,
            Climate.TargetPoint mixedNoisePoint) {
        Objects.requireNonNull(mixedNoisePoint, "MultiNoiseUtil.NoiseValuePoint is null");
        addNetherBiome(biome, Climate.parameters(
                (float) mixedNoisePoint.temperature(),
                (float) mixedNoisePoint.humidity(),
                (float) mixedNoisePoint.continentalness(),
                (float) mixedNoisePoint.erosion(),
                (float) mixedNoisePoint.depth(),
                (float) mixedNoisePoint.weirdness(),
                0.0F));
    }

    public static void addNetherBiome(ResourceKey<Biome> biome,
            Climate.ParameterPoint mixedNoisePoint) {
        NetherBiomeEntries.add(biome, mixedNoisePoint);
    }

    public static boolean canGenerateInNether(ResourceKey<Biome> biome) {
        return NetherBiomeEntries.canGenerate(biome);
    }
}
