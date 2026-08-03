package net.fabricmc.fabric.api.client.rendering.v1;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Fabric block-entity renderer registration backed by Minecraft's shared registry. */
public final class BlockEntityRendererRegistry {
    private BlockEntityRendererRegistry() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <E extends BlockEntity> void register(BlockEntityType<E> blockEntityType,
            BlockEntityRendererProvider<? super E> rendererFactory) {
        BlockEntityRenderers.register(blockEntityType, (BlockEntityRendererProvider) rendererFactory);
    }
}
