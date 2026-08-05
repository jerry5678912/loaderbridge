package dev.loaderbridge.fabric.api.biome.mixin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.loaderbridge.fabric.api.biome.EndBiomeEntries;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TheEndBiomeSource.class)
public abstract class TheEndBiomeSourceMixin {
    @Shadow @Final @Mutable private static MapCodec<TheEndBiomeSource> CODEC;
    @Unique private EndBiomeEntries.Overrides loaderbridge$overrides;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void loaderbridge$useRegistryAwareCodec(CallbackInfo callback) {
        CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                RegistryOps.<Biome, TheEndBiomeSource>retrieveGetter(Registries.BIOME))
                .apply(instance, instance.stable(TheEndBiomeSource::create)));
    }

    @Inject(method = "create", at = @At("HEAD"))
    private static void loaderbridge$captureLookup(HolderGetter<Biome> biomes,
            CallbackInfoReturnable<TheEndBiomeSource> callback) {
        EndBiomeEntries.rememberLookup(biomes);
    }

    @Inject(method = "create", at = @At("RETURN"))
    private static void loaderbridge$clearLookup(HolderGetter<Biome> biomes,
            CallbackInfoReturnable<TheEndBiomeSource> callback) {
        EndBiomeEntries.clearLookup();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void loaderbridge$createOverrides(Holder<Biome> end, Holder<Biome> highlands,
            Holder<Biome> midlands, Holder<Biome> islands, Holder<Biome> barrens,
            CallbackInfo callback) {
        loaderbridge$overrides = EndBiomeEntries.createOverrides();
    }

    @Inject(method = "collectPossibleBiomes", at = @At("RETURN"), cancellable = true)
    private void loaderbridge$addPossibleBiomes(
            CallbackInfoReturnable<Stream<Holder<Biome>>> callback) {
        callback.setReturnValue(Stream.concat(callback.getReturnValue(),
                loaderbridge$overrides.customBiomes().stream()));
    }

    @Inject(method = "getNoiseBiome", at = @At("RETURN"), cancellable = true)
    private void loaderbridge$pickWeightedBiome(int x, int y, int z, Climate.Sampler sampler,
            CallbackInfoReturnable<Holder<Biome>> callback) {
        callback.setReturnValue(loaderbridge$overrides.pick(x, z, sampler,
                callback.getReturnValue()));
    }
}
