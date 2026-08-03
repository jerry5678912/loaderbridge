package net.fabricmc.fabric.api.transfer.v1.item;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;

/** Standard block and item lookups for item-variant storage. */
public final class ItemStorage {
    public static final BlockApiLookup<Storage<ItemVariant>, Direction> SIDED =
            BlockApiLookup.get(ResourceLocation.fromNamespaceAndPath(
                    "fabric", "sided_item_storage"), Storage.asClass(), Direction.class);

    public static final ItemApiLookup<Storage<ItemVariant>, ContainerItemContext> ITEM =
            ItemApiLookup.get(ResourceLocation.fromNamespaceAndPath(
                    "fabric", "item_storage"), Storage.asClass(), ContainerItemContext.class);

    static {
        SIDED.registerFallback((world, pos, state, blockEntity, direction) ->
                blockEntity instanceof Container inventory
                        ? InventoryStorage.of(inventory, direction) : null);
    }

    private ItemStorage() {
    }
}
