package net.fabricmc.fabric.api.util;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

/** Mutable item and item-tag overrides used by vanilla-mechanic registries. */
public interface Item2ObjectMap<V> {
    V get(ItemLike item);
    void add(ItemLike item, V value);
    void add(TagKey<Item> tag, V value);
    void remove(ItemLike item);
    void remove(TagKey<Item> tag);
    void clear(ItemLike item);
    void clear(TagKey<Item> tag);
}
