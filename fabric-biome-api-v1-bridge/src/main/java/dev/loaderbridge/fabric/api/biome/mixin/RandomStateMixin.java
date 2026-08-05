package dev.loaderbridge.fabric.api.biome.mixin;

import dev.loaderbridge.fabric.api.biome.EndClimateSampler;
import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RandomState.class)
public abstract class RandomStateMixin {
    @Shadow @Final private Climate.Sampler sampler;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void loaderbridge$rememberSeed(NoiseGeneratorSettings settings,
            HolderGetter<NormalNoise.NoiseParameters> noises, long seed, CallbackInfo callback) {
        ((EndClimateSampler) (Object) sampler).loaderbridge$setSeed(seed);
    }
}
