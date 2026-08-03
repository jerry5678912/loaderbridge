package dev.loaderbridge.fabric.api.lifecycle.mixin;

import dev.loaderbridge.fabric.api.lifecycle.ChunkLifecycleTracker;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkStatusTasks.class)
abstract class ChunkStatusTasksMixin {
    @Unique
    private static final FullChunkStatus[] LOADERBRIDGE_STATUSES = FullChunkStatus.values();

    @Inject(method = "lambda$full$2", at = @At("TAIL"))
    private static void loaderbridge$onChunkLoad(ChunkAccess original, WorldGenContext context,
            GenerationChunkHolder holder, CallbackInfoReturnable<ChunkAccess> callback) {
        LevelChunk chunk = (LevelChunk) callback.getReturnValue();
        ServerChunkEvents.CHUNK_LOAD.invoker().onChunkLoad(context.level(), chunk);
        if (!(original instanceof ImposterProtoChunk)) {
            ServerChunkEvents.CHUNK_GENERATE.invoker().onChunkGenerate(context.level(), chunk);
        }

        ChunkLifecycleTracker tracker = (ChunkLifecycleTracker) holder;
        for (int index = tracker.loaderbridge$getCurrentEventStatus().ordinal();
                index < holder.getFullStatus().ordinal(); index++) {
            FullChunkStatus previous = LOADERBRIDGE_STATUSES[index];
            FullChunkStatus current = LOADERBRIDGE_STATUSES[index + 1];
            ServerChunkEvents.CHUNK_LEVEL_TYPE_CHANGE.invoker()
                    .onChunkLevelTypeChange(context.level(), chunk, previous, current);
            tracker.loaderbridge$setCurrentEventStatus(current);
        }
    }
}
