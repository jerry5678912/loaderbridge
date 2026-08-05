package net.fabricmc.fabric.api.biome.v1;

import dev.loaderbridge.fabric.api.biome.EndBiomeEntries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public final class TheEndBiomes {
    private TheEndBiomes() { }

    public static void addMainIslandBiome(ResourceKey<Biome> biome, double weight) {
        EndBiomeEntries.addReplacement(Biomes.THE_END, biome, weight);
    }

    public static void addHighlandsBiome(ResourceKey<Biome> biome, double weight) {
        EndBiomeEntries.addReplacement(Biomes.END_HIGHLANDS, biome, weight);
    }

    public static void addSmallIslandsBiome(ResourceKey<Biome> biome, double weight) {
        EndBiomeEntries.addReplacement(Biomes.SMALL_END_ISLANDS, biome, weight);
    }

    public static void addMidlandsBiome(ResourceKey<Biome> highlands,
            ResourceKey<Biome> midlands, double weight) {
        EndBiomeEntries.addMidlands(highlands, midlands, weight);
    }

    public static void addBarrensBiome(ResourceKey<Biome> highlands,
            ResourceKey<Biome> barrens, double weight) {
        EndBiomeEntries.addBarrens(highlands, barrens, weight);
    }
}
