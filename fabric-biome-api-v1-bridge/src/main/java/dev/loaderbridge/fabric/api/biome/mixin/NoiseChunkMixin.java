package dev.loaderbridge.fabric.api.biome.mixin;

import dev.loaderbridge.fabric.api.biome.EndClimateSampler;
import java.util.List;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NoiseChunk.class)
public abstract class NoiseChunkMixin {
    @Unique private long loaderbridge$seed;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void loaderbridge$captureSeed(int cellCountXZ, RandomState randomState,
            int firstCellX, int firstCellZ, NoiseSettings noiseSettings,
            DensityFunctions.BeardifierOrMarker beardifier,
            NoiseGeneratorSettings generatorSettings, Aquifer.FluidPicker fluidPicker,
            Blender blender, CallbackInfo callback) {
        loaderbridge$seed = ((EndClimateSampler) (Object) randomState.sampler())
                .loaderbridge$getSeed();
    }

    @Inject(method = "cachedClimateSampler", at = @At("RETURN"))
    private void loaderbridge$seedCachedSampler(NoiseRouter router,
            List<Climate.ParameterPoint> spawnTargets,
            CallbackInfoReturnable<Climate.Sampler> callback) {
        ((EndClimateSampler) (Object) callback.getReturnValue())
                .loaderbridge$setSeed(loaderbridge$seed);
    }
}
