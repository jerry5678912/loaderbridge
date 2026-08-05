package dev.loaderbridge.fixture.attachment.mixin;

import dev.loaderbridge.fixture.attachment.FabricDataAttachmentFixture;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkStatusTasks.class)
abstract class GeneratedProtoChunkMixin {
    @Inject(method = "lambda$full$2", at = @At("HEAD"))
    private static void loaderbridge$seedGeneratedProtoChunk(ChunkAccess original,
            WorldGenContext context, GenerationChunkHolder holder,
            CallbackInfoReturnable<ChunkAccess> callback) {
        if (!(original instanceof ImposterProtoChunk)) {
            ((AttachmentTarget) original).setAttached(
                    FabricDataAttachmentFixture.GENERATED_PROTO_CHUNK, 79);
        }
    }
}
