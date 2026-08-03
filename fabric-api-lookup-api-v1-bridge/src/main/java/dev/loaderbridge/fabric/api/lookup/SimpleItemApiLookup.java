package dev.loaderbridge.fabric.api.lookup;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.ItemStack;

/** Provider-first Fabric item lookup implementation. */
final class SimpleItemApiLookup<A, C> implements ItemApiLookup<A, C> {
    private final ResourceLocation id;
    private final Class<A> apiClass;
    private final Class<C> contextClass;
    private final Map<Item, ItemApiProvider<A, C>> providers = new ConcurrentHashMap<>();
    private final List<ItemApiProvider<A, C>> fallbacks = new CopyOnWriteArrayList<>();

    @SuppressWarnings("unchecked")
    SimpleItemApiLookup(ResourceLocation id, Class<?> apiClass, Class<?> contextClass) {
        this.id = id; this.apiClass = (Class<A>) apiClass; this.contextClass = (Class<C>) contextClass;
    }

    @Override public A find(ItemStack stack, C context) {
        Objects.requireNonNull(stack, "ItemStack may not be null.");
        ItemApiProvider<A, C> provider = providers.get(stack.getItem());
        if (provider != null) {
            A result = provider.find(stack, context);
            if (result != null) return result;
        }
        for (ItemApiProvider<A, C> fallback : fallbacks) {
            A result = fallback.find(stack, context);
            if (result != null) return result;
        }
        return null;
    }

    @Override @SuppressWarnings("unchecked") public void registerSelf(ItemLike... items) {
        for (ItemLike itemLike : checked(items)) {
            Item item = itemLike.asItem();
            if (!apiClass.isAssignableFrom(item.getClass())) {
                throw new IllegalArgumentException("API class " + apiClass.getCanonicalName()
                        + " is not assignable from item class " + item.getClass().getCanonicalName());
            }
        }
        registerForItems((stack, context) -> apiClass.cast(stack.getItem()), items);
    }

    @Override public void registerForItems(ItemApiProvider<A, C> provider, ItemLike... items) {
        Objects.requireNonNull(provider, "ItemApiProvider may not be null.");
        for (ItemLike item : checked(items)) providers.putIfAbsent(item.asItem(), provider);
    }

    private static ItemLike[] checked(ItemLike[] items) {
        if (items.length == 0) throw new IllegalArgumentException("Must register at least one item");
        for (ItemLike item : items) Objects.requireNonNull(item, "Encountered null item");
        return items;
    }

    @Override public void registerFallback(ItemApiProvider<A, C> provider) { fallbacks.add(Objects.requireNonNull(provider)); }
    @Override public ResourceLocation getId() { return id; }
    @Override public Class<A> apiClass() { return apiClass; }
    @Override public Class<C> contextClass() { return contextClass; }
    @Override public ItemApiProvider<A, C> getProvider(Item item) { return providers.get(item); }
}
