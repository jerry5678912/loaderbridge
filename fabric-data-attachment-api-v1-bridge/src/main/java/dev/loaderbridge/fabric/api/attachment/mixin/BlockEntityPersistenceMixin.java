package dev.loaderbridge.fabric.api.attachment.mixin;

import net.fabricmc.fabric.impl.attachment.AttachmentTargetImpl;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
abstract class BlockEntityPersistenceMixin implements AttachmentTargetImpl {
    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void loaderbridge$readAttachments(CompoundTag tag, HolderLookup.Provider registries,
            CallbackInfo callback) {
        fabric_readAttachments(tag, registries.createSerializationContext(NbtOps.INSTANCE));
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void loaderbridge$writeAttachments(CompoundTag tag, HolderLookup.Provider registries,
            CallbackInfo callback) {
        fabric_writeAttachments(tag, registries.createSerializationContext(NbtOps.INSTANCE));
    }
}
