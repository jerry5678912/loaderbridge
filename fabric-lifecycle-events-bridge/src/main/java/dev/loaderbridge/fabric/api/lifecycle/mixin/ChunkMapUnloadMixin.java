package dev.loaderbridge.fabric.api.lifecycle.mixin;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkMap.class)
abstract class ChunkMapUnloadMixin {
    @Shadow @Final private ServerLevel level;

    @Redirect(method = "lambda$scheduleUnload$12", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/LevelChunk;setLoaded(Z)V"))
    private void loaderbridge$onChunkUnload(LevelChunk chunk, boolean loaded,
            ChunkHolder holder, long position) {
        chunk.setLoaded(loaded);
        if (!loaded) {
            ServerChunkEvents.CHUNK_UNLOAD.invoker().onChunkUnload(level, chunk);
        }
    }
}
