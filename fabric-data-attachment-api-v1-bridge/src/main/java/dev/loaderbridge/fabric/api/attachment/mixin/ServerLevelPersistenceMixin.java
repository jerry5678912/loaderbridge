package dev.loaderbridge.fabric.api.attachment.mixin;

import net.fabricmc.fabric.impl.attachment.AttachmentPersistentState;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
abstract class ServerLevelPersistenceMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void loaderbridge$createAttachmentState(CallbackInfo callback) {
        AttachmentPersistentState.getOrCreate((ServerLevel) (Object) this);
    }
}
