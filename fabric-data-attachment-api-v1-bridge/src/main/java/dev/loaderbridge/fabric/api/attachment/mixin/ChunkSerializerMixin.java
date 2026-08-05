package dev.loaderbridge.fabric.api.attachment.mixin;

import net.fabricmc.fabric.impl.attachment.AttachmentTargetImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkSerializer.class)
abstract class ChunkSerializerMixin {
    @Inject(method = "write", at = @At("RETURN"))
    private static void loaderbridge$writeAttachments(ServerLevel level, ChunkAccess chunk,
            CallbackInfoReturnable<CompoundTag> callback) {
        ((AttachmentTargetImpl) chunk).fabric_writeAttachments(callback.getReturnValue(),
                level.registryAccess().createSerializationContext(NbtOps.INSTANCE));
    }

    @Inject(method = "read", at = @At("RETURN"))
    private static void loaderbridge$readAttachments(ServerLevel level, PoiManager poiManager,
            RegionStorageInfo storageInfo, ChunkPos position, CompoundTag tag,
            CallbackInfoReturnable<ProtoChunk> callback) {
        ProtoChunk returned = callback.getReturnValue();
        ChunkAccess target = returned instanceof ImposterProtoChunk imposter
                ? imposter.getWrapped() : returned;
        ((AttachmentTargetImpl) target).fabric_readAttachments(tag,
                level.registryAccess().createSerializationContext(NbtOps.INSTANCE));
    }
}
