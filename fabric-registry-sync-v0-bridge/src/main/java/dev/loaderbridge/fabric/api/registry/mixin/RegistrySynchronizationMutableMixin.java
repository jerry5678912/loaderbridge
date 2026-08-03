package dev.loaderbridge.fabric.api.registry.mixin;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RegistrySynchronization.class)
abstract class RegistrySynchronizationMutableMixin {
    @Shadow @Final @Mutable
    public static Set<ResourceKey<? extends Registry<?>>> NETWORKABLE_REGISTRIES;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void loaderbridge$makeNetworkableRegistriesMutable(CallbackInfo callback) {
        NETWORKABLE_REGISTRIES = new HashSet<>(NETWORKABLE_REGISTRIES);
    }
}
