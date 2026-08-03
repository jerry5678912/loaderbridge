package dev.loaderbridge.fabric.api.lifecycle.mixin;

import dev.loaderbridge.fabric.api.lifecycle.ChunkLifecycleTracker;
import java.util.concurrent.Executor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkHolder.class)
abstract class ChunkHolderLifecycleMixin extends GenerationChunkHolder implements ChunkLifecycleTracker {
    @Shadow @Final private LevelHeightAccessor levelHeightAccessor;
    @Shadow private int oldTicketLevel;

    @Unique
    private static final FullChunkStatus[] LOADERBRIDGE_STATUSES = FullChunkStatus.values();
    @Unique
    private FullChunkStatus loaderbridge$currentEventStatus = FullChunkStatus.INACCESSIBLE;

    private ChunkHolderLifecycleMixin(ChunkPos position) {
        super(position);
    }

    @Inject(method = "updateFutures", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ChunkHolder;addSaveDependency(Ljava/util/concurrent/CompletableFuture;)V",
            shift = At.Shift.AFTER, ordinal = 0))
    private void loaderbridge$inaccessibleToFull(ChunkMap map, Executor executor, CallbackInfo callback) {
        if (getChunkIfPresentUnchecked(ChunkStatus.FULL) instanceof LevelChunk chunk
                && loaderbridge$currentEventStatus == FullChunkStatus.INACCESSIBLE) {
            loaderbridge$fire(chunk, FullChunkStatus.INACCESSIBLE, FullChunkStatus.FULL);
        }
    }

    @Inject(method = "updateFutures", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ChunkHolder;addSaveDependency(Ljava/util/concurrent/CompletableFuture;)V",
            shift = At.Shift.AFTER, ordinal = 1))
    private void loaderbridge$fullToBlockTicking(ChunkMap map, Executor executor, CallbackInfo callback) {
        if (loaderbridge$currentEventStatus == FullChunkStatus.FULL
                && getChunkIfPresentUnchecked(ChunkStatus.FULL) instanceof LevelChunk chunk) {
            loaderbridge$fire(chunk, FullChunkStatus.FULL, FullChunkStatus.BLOCK_TICKING);
        }
    }

    @Inject(method = "updateFutures", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ChunkHolder;addSaveDependency(Ljava/util/concurrent/CompletableFuture;)V",
            shift = At.Shift.AFTER, ordinal = 2))
    private void loaderbridge$blockToEntityTicking(ChunkMap map, Executor executor, CallbackInfo callback) {
        if (loaderbridge$currentEventStatus == FullChunkStatus.BLOCK_TICKING
                && getChunkIfPresentUnchecked(ChunkStatus.FULL) instanceof LevelChunk chunk) {
            loaderbridge$fire(chunk, FullChunkStatus.BLOCK_TICKING, FullChunkStatus.ENTITY_TICKING);
        }
    }

    @Inject(method = "demoteFullChunk", at = @At("HEAD"))
    private void loaderbridge$demote(ChunkMap map, FullChunkStatus target, CallbackInfo callback) {
        FullChunkStatus previous = ChunkLevel.fullStatus(oldTicketLevel);
        if (!(getChunkIfPresentUnchecked(ChunkStatus.FULL) instanceof LevelChunk chunk)) {
            return;
        }
        for (int index = previous.ordinal(); index > target.ordinal(); index--) {
            FullChunkStatus oldStatus = LOADERBRIDGE_STATUSES[index];
            FullChunkStatus newStatus = LOADERBRIDGE_STATUSES[index - 1];
            if (loaderbridge$currentEventStatus.isOrAfter(oldStatus)) {
                loaderbridge$fire(chunk, oldStatus, newStatus);
            }
        }
    }

    @Unique
    private void loaderbridge$fire(LevelChunk chunk, FullChunkStatus previous, FullChunkStatus current) {
        ServerChunkEvents.CHUNK_LEVEL_TYPE_CHANGE.invoker().onChunkLevelTypeChange(
                (ServerLevel) levelHeightAccessor, chunk, previous, current);
        loaderbridge$currentEventStatus = current;
    }

    @Override
    public FullChunkStatus loaderbridge$getCurrentEventStatus() {
        return loaderbridge$currentEventStatus;
    }

    @Override
    public void loaderbridge$setCurrentEventStatus(FullChunkStatus status) {
        loaderbridge$currentEventStatus = status;
    }
}
