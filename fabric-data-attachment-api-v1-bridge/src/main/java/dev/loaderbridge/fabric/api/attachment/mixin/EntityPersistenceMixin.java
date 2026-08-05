package dev.loaderbridge.fabric.api.attachment.mixin;

import net.fabricmc.fabric.impl.attachment.AttachmentTargetImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
abstract class EntityPersistenceMixin implements AttachmentTargetImpl {
    @Inject(method = "load", at = @At("TAIL"))
    private void loaderbridge$readAttachments(CompoundTag tag, CallbackInfo callback) {
        Entity self = (Entity) (Object) this;
        fabric_readAttachments(tag,
                self.registryAccess().createSerializationContext(NbtOps.INSTANCE));
    }

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void loaderbridge$writeAttachments(CompoundTag tag,
            CallbackInfoReturnable<CompoundTag> callback) {
        Entity self = (Entity) (Object) this;
        fabric_writeAttachments(callback.getReturnValue(),
                self.registryAccess().createSerializationContext(NbtOps.INSTANCE));
    }
}
