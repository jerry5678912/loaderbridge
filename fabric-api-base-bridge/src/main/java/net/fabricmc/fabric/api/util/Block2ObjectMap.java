package net.fabricmc.fabric.api.util;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Mutable block and block-tag overrides used by vanilla-mechanic registries. */
public interface Block2ObjectMap<V> {
    V get(Block block);
    void add(Block block, V value);
    void add(TagKey<Block> tag, V value);
    void remove(Block block);
    void remove(TagKey<Block> tag);
    void clear(Block block);
    void clear(TagKey<Block> tag);
}
