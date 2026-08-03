package dev.loaderbridge.fabric.api.lifecycle.mixin;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fires after server tracking starts and before it ends, matching Fabric's lifecycle timing. */
@Mixin(targets = "net.minecraft.server.level.ServerLevel$EntityCallbacks")
abstract class ServerLevelEntityCallbacksMixin {
    @Shadow
    @Final
    private ServerLevel this$0;

    @Inject(method = "onTrackingStart", at = @At("TAIL"))
    private void loaderbridge$entityLoaded(Entity entity, CallbackInfo callback) {
        ServerEntityEvents.ENTITY_LOAD.invoker().onLoad(entity, this$0);
    }

    @Inject(method = "onTrackingEnd", at = @At("HEAD"))
    private void loaderbridge$entityUnloading(Entity entity, CallbackInfo callback) {
        ServerEntityEvents.ENTITY_UNLOAD.invoker().onUnload(entity, this$0);
    }
}
