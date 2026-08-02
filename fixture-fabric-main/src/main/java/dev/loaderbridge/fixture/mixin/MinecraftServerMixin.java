package dev.loaderbridge.fixture.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.server.MinecraftServer")
abstract class MinecraftServerMixin {
    @Inject(method = "runServer", at = @At("HEAD"))
    private void loaderbridge$markServerRun(CallbackInfo callback) {
        System.out.println("LOADERBRIDGE_FIXTURE_MIXIN_APPLIED");
    }
}
