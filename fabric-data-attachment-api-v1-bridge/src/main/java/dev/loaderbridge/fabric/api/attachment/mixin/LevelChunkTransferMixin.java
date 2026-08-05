package dev.loaderbridge.fabric.api.attachment.mixin;

import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.impl.attachment.AttachmentTargetImpl;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
abstract class LevelChunkTransferMixin {
    @Inject(method = "<init>(Lnet/minecraft/server/level/ServerLevel;"
            + "Lnet/minecraft/world/level/chunk/ProtoChunk;"
            + "Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V",
            at = @At("TAIL"))
    private void loaderbridge$transferAttachments(ServerLevel level, ProtoChunk protoChunk,
            LevelChunk.PostLoadProcessor postLoad, CallbackInfo callback) {
        AttachmentTargetImpl.transfer((AttachmentTarget) (Object) protoChunk,
                (AttachmentTarget) (Object) this, false);
    }
}
