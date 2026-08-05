package dev.loaderbridge.fabric.api.biome.mixin;

import dev.loaderbridge.fabric.api.biome.NetherBiomeEntries;
import java.util.function.Function;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList$Preset$1")
public final class NetherBiomePresetMixin {
    @Inject(method = "apply", at = @At("RETURN"), cancellable = true)
    private <T> void loaderbridge$addNetherBiomes(Function<ResourceKey<Biome>, T> biomes,
            CallbackInfoReturnable<Climate.ParameterList<T>> callback) {
        callback.setReturnValue(NetherBiomeEntries.withModdedEntries(
                callback.getReturnValue(), biomes));
    }
}
