package net.fabricmc.fabric.api.client.rendering.v1;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;

public interface ColorProviderRegistry<T, Provider> {
    ColorProviderRegistry<ItemLike, ItemColor> ITEM = new ItemRegistry();
    ColorProviderRegistry<Block, BlockColor> BLOCK = new BlockRegistry();

    @SuppressWarnings("unchecked")
    void register(Provider provider, T... objects);
    Provider get(T object);

    final class BlockRegistry implements ColorProviderRegistry<Block, BlockColor> {
        private final Map<Block, BlockColor> providers = new IdentityHashMap<>();
        private BlockRegistry() { }
        @Override public synchronized void register(BlockColor provider, Block... blocks) {
            for (Block block : blocks) providers.put(block, provider);
        }
        @Override public synchronized BlockColor get(Block block) { return providers.get(block); }
        public synchronized void registerTo(RegisterColorHandlersEvent.Block event) {
            providers.forEach((block, provider) -> event.register(provider, block));
        }
    }

    final class ItemRegistry implements ColorProviderRegistry<ItemLike, ItemColor> {
        private final Map<Item, ItemColor> providers = new IdentityHashMap<>();
        private ItemRegistry() { }
        @Override public synchronized void register(ItemColor provider, ItemLike... items) {
            for (ItemLike item : items) providers.put(item.asItem(), provider);
        }
        @Override public synchronized ItemColor get(ItemLike item) {
            return providers.get(item.asItem());
        }
        public synchronized void registerTo(RegisterColorHandlersEvent.Item event) {
            providers.forEach((item, provider) -> event.register(provider, item));
        }
    }
}
