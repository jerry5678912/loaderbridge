package dev.loaderbridge.fabric.api.interaction.mixin;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
    @Shadow private ServerPlayer player;

    @Inject(method = "setPlayer", at = @At("HEAD"), cancellable = true)
    private void loaderbridge$preserveRealOwner(ServerPlayer newPlayer, CallbackInfo callback) {
        if (newPlayer instanceof FakePlayer) callback.cancel();
    }

    @Inject(method = "award", at = @At("HEAD"), cancellable = true)
    private void loaderbridge$rejectFakePlayerAward(
            AdvancementHolder advancement, String criterion,
            CallbackInfoReturnable<Boolean> callback) {
        if (player instanceof FakePlayer) callback.setReturnValue(false);
    }
}
