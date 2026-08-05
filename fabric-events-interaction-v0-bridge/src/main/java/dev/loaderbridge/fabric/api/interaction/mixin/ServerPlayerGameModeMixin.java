package dev.loaderbridge.fabric.api.interaction.mixin;

import dev.loaderbridge.fabric.api.interaction.BridgeInteractionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void loaderbridge$beginFabricBreak(
            BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        BridgeInteractionEvents.clearPendingBreak();
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void loaderbridge$finishFabricBreak(
            BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        BridgeInteractionEvents.finishPendingBreak(pos, callback.getReturnValue());
    }
}
