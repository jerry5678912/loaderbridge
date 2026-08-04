package dev.loaderbridge.fabric.entity.events.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer {
    @Shadow @Final public ClientPacketListener connection;

    protected LocalPlayerMixin(ClientLevel level, GameProfile profile) {
        super(level, profile);
        throw new AssertionError();
    }

    @Inject(method = "aiStep", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/EquipmentSlot;CHEST:"
                    + "Lnet/minecraft/world/entity/EquipmentSlot;"),
            slice = @Slice(
                    from = @At(value = "INVOKE",
                            target = "Lnet/minecraft/client/player/LocalPlayer;onClimbable()Z"),
                    to = @At(value = "INVOKE",
                            target = "Lnet/minecraft/client/player/LocalPlayer;"
                                    + "tryToStartFallFlying()Z")))
    private void loaderbridge$startCustomElytraFlight(CallbackInfo callback) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (this.tryToStartFallFlying()) {
            connection.send(new ServerboundPlayerCommandPacket(
                    self, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        }
    }
}
