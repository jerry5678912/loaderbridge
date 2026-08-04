package dev.loaderbridge.fabric.entity.events.mixin;

import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Shadow public abstract void startFallFlying();

    @Inject(method = "tryToStartFallFlying", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/EquipmentSlot;CHEST:"
                    + "Lnet/minecraft/world/entity/EquipmentSlot;"), cancellable = true)
    private void loaderbridge$tryToStartFallFlying(CallbackInfoReturnable<Boolean> callback) {
        Player self = (Player) (Object) this;
        if (!EntityElytraEvents.ALLOW.invoker().allowElytraFlight(self)) {
            callback.setReturnValue(false);
        } else if (EntityElytraEvents.CUSTOM.invoker().useCustomElytra(self, false)) {
            startFallFlying();
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "isSleepingLongEnough", at = @At("RETURN"), cancellable = true)
    private void loaderbridge$allowResettingTime(CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ()) {
            callback.setReturnValue(EntitySleepEvents.ALLOW_RESETTING_TIME.invoker()
                    .allowResettingTime((Player) (Object) this));
        }
    }
}
