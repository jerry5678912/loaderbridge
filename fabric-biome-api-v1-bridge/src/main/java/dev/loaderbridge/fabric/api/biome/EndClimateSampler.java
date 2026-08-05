package dev.loaderbridge.fabric.api.biome;

import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public interface EndClimateSampler {
    void loaderbridge$setSeed(long seed);
    long loaderbridge$getSeed();
    SimplexNoise loaderbridge$getEndBiomeNoise();
}
