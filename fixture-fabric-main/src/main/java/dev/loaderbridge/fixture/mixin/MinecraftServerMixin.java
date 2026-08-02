package dev.loaderbridge.fixture.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.server.MinecraftServer")
abstract class MinecraftServerMixin {
    @Unique
    private static boolean loaderbridge$mixinExtrasReported;

    @Inject(method = "runServer", at = @At("HEAD"))
    private void loaderbridge$markServerRun(CallbackInfo callback) {
        System.out.println("LOADERBRIDGE_FIXTURE_MIXIN_APPLIED");
    }

    @ModifyReturnValue(method = "isStopped", at = @At("RETURN"))
    private boolean loaderbridge$markMixinExtras(boolean original) {
        if (!loaderbridge$mixinExtrasReported) {
            loaderbridge$mixinExtrasReported = true;
            System.out.println("LOADERBRIDGE_FIXTURE_MIXIN_EXTRAS_APPLIED");
        }
        return original;
    }
}
