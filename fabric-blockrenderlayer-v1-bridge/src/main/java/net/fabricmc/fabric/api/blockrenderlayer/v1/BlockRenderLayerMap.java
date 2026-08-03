package net.fabricmc.fabric.api.blockrenderlayer.v1;

import java.util.Objects;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public interface BlockRenderLayerMap {
    BlockRenderLayerMap INSTANCE = new ForgeBlockRenderLayerMap();

    void putBlock(Block block, RenderType renderLayer);
    void putBlocks(RenderType renderLayer, Block... blocks);
    void putItem(Item item, RenderType renderLayer);
    void putItems(RenderType renderLayer, Item... items);
    void putFluid(Fluid fluid, RenderType renderLayer);
    void putFluids(RenderType renderLayer, Fluid... fluids);

    final class ForgeBlockRenderLayerMap implements BlockRenderLayerMap {
        private ForgeBlockRenderLayerMap() { }
        @SuppressWarnings("removal")
        @Override public void putBlock(Block block, RenderType renderLayer) {
            ItemBlockRenderTypes.setRenderLayer(Objects.requireNonNull(block, "block"),
                    Objects.requireNonNull(renderLayer, "renderLayer"));
        }
        @Override public void putBlocks(RenderType renderLayer, Block... blocks) {
            for (Block block : blocks) putBlock(block, renderLayer);
        }
        @Override public void putItem(Item item, RenderType renderLayer) {
            putBlock(Block.byItem(Objects.requireNonNull(item, "item")), renderLayer);
        }
        @Override public void putItems(RenderType renderLayer, Item... items) {
            for (Item item : items) putItem(item, renderLayer);
        }
        @Override public void putFluid(Fluid fluid, RenderType renderLayer) {
            ItemBlockRenderTypes.setRenderLayer(Objects.requireNonNull(fluid, "fluid"),
                    Objects.requireNonNull(renderLayer, "renderLayer"));
        }
        @Override public void putFluids(RenderType renderLayer, Fluid... fluids) {
            for (Fluid fluid : fluids) putFluid(fluid, renderLayer);
        }
    }
}
