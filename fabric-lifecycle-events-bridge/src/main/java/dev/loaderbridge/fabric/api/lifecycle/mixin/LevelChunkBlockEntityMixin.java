package dev.loaderbridge.fabric.api.lifecycle.mixin;

import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Preserves Fabric's replacement and removal semantics at the block-entity map boundary. */
@Mixin(LevelChunk.class)
abstract class LevelChunkBlockEntityMixin {
    @Redirect(method = "setBlockEntity",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object loaderbridge$replaceBlockEntity(Map<BlockPos, BlockEntity> entities,
            Object position, Object value) {
        BlockPos blockPos = (BlockPos) position;
        BlockEntity blockEntity = (BlockEntity) value;
        BlockEntity previous = entities.put(blockPos, blockEntity);
        if (((LevelChunk) (Object) this).getLevel() instanceof ServerLevel world
                && blockEntity != previous) {
            if (previous != null) {
                ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.invoker().onUnload(previous, world);
            }
            ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.invoker().onLoad(blockEntity, world);
        }
        return previous;
    }

    @Redirect(method = {
            "getBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/chunk/LevelChunk$EntityCreationType;)Lnet/minecraft/world/level/block/entity/BlockEntity;",
            "removeBlockEntity"
    }, at = @At(value = "INVOKE", target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;",
            ordinal = 0))
    private Object loaderbridge$removeBlockEntity(Map<BlockPos, BlockEntity> entities, Object position) {
        BlockEntity removed = entities.remove(position);
        if (removed != null && ((LevelChunk) (Object) this).getLevel() instanceof ServerLevel world) {
            ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.invoker().onUnload(removed, world);
        }
        return removed;
    }
}
