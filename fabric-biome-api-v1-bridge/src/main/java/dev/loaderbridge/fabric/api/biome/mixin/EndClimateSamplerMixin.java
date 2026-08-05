package dev.loaderbridge.fabric.api.biome.mixin;

import dev.loaderbridge.fabric.api.biome.EndClimateSampler;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Climate.Sampler.class)
public abstract class EndClimateSamplerMixin implements EndClimateSampler {
    @Unique private Long loaderbridge$seed;
    @Unique private SimplexNoise loaderbridge$endBiomeNoise;

    @Override
    public void loaderbridge$setSeed(long seed) {
        loaderbridge$seed = seed;
        loaderbridge$endBiomeNoise = null;
    }

    @Override
    public long loaderbridge$getSeed() {
        if (loaderbridge$seed == null) {
            throw new IllegalStateException("Climate sampler seed is unavailable");
        }
        return loaderbridge$seed;
    }

    @Override
    public SimplexNoise loaderbridge$getEndBiomeNoise() {
        if (loaderbridge$endBiomeNoise == null) {
            loaderbridge$endBiomeNoise = new SimplexNoise(new WorldgenRandom(
                    new LegacyRandomSource(loaderbridge$getSeed())));
        }
        return loaderbridge$endBiomeNoise;
    }
}
