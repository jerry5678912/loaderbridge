package dev.loaderbridge.fabric.api.attachment.mixin;

import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.impl.attachment.sync.AttachmentSyncRuntime;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerEntity.class)
abstract class ServerEntityPairingMixin {
    @Shadow @Final private Entity entity;

    @Inject(
            method = "addPairing",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;startSeenByPlayer(Lnet/minecraft/server/level/ServerPlayer;)V"))
    private void loaderbridge$syncAttachmentsAfterSpawn(
            ServerPlayer player, CallbackInfo callbackInfo) {
        AttachmentSyncRuntime.syncInitial((AttachmentTarget) (Object) entity, player);
    }
}
