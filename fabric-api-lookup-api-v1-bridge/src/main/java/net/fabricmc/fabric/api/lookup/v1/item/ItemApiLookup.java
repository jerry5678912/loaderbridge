package net.fabricmc.fabric.api.lookup.v1.item;

import dev.loaderbridge.fabric.api.lookup.ItemApiLookupRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.ItemStack;

/** Binary-compatible Fabric item API lookup contract. */
public interface ItemApiLookup<A, C> {
    static <A, C> ItemApiLookup<A, C> get(ResourceLocation id, Class<A> apiClass,
            Class<C> contextClass) {
        return ItemApiLookupRegistry.get(id, apiClass, contextClass);
    }

    A find(ItemStack stack, C context);

    void registerSelf(ItemLike... items);

    void registerForItems(ItemApiProvider<A, C> provider, ItemLike... items);

    void registerFallback(ItemApiProvider<A, C> fallbackProvider);

    ResourceLocation getId();

    Class<A> apiClass();

    Class<C> contextClass();

    ItemApiProvider<A, C> getProvider(Item item);

    @FunctionalInterface
    interface ItemApiProvider<A, C> {
        A find(ItemStack stack, C context);
    }
}
