package dev.loaderbridge.fabric.api.registry.mixin;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.RegistryDataLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RegistryDataLoader.class)
abstract class RegistryDataLoaderMutableMixin {
    @Shadow @Final @Mutable
    public static List<RegistryDataLoader.RegistryData<?>> SYNCHRONIZED_REGISTRIES;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void loaderbridge$makeSynchronizedRegistriesMutable(CallbackInfo callback) {
        SYNCHRONIZED_REGISTRIES = new ArrayList<>(SYNCHRONIZED_REGISTRIES);
    }
}
