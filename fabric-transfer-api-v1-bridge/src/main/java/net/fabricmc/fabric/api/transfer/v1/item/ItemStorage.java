package net.fabricmc.fabric.api.transfer.v1.item;

import dev.loaderbridge.fabric.api.transfer.item.BridgeBundleContentsStorage;
import dev.loaderbridge.fabric.api.transfer.item.BridgeComposterStorage;
import dev.loaderbridge.fabric.api.transfer.item.BridgeContainerComponentStorage;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;

/** Standard block and item lookups for item-variant storage. */
public final class ItemStorage {
    public static final BlockApiLookup<Storage<ItemVariant>, Direction> SIDED =
            BlockApiLookup.get(ResourceLocation.fromNamespaceAndPath(
                    "fabric", "sided_item_storage"), Storage.asClass(), Direction.class);

    public static final ItemApiLookup<Storage<ItemVariant>, ContainerItemContext> ITEM =
            ItemApiLookup.get(ResourceLocation.fromNamespaceAndPath(
                    "fabric", "item_storage"), Storage.asClass(), ContainerItemContext.class);

    static {
        SIDED.registerForBlocks(BridgeComposterStorage::find, Blocks.COMPOSTER);
        SIDED.registerFallback((world, pos, state, blockEntity, direction) ->
                blockEntity instanceof SidedStorageBlockEntity provider
                        ? provider.getItemStorage(direction) : null);
        SIDED.registerFallback((world, pos, state, blockEntity, direction) -> {
            if (!(state.getBlock() instanceof WorldlyContainerHolder holder)) return null;
            Container first = holder.getContainer(state, world, pos);
            Container second = holder.getContainer(state, world, pos);
            return first != null && first == second ? InventoryStorage.of(first, direction) : null;
        });
        SIDED.registerFallback((world, pos, state, blockEntity, direction) -> {
            if (!(blockEntity instanceof Container inventory)) return null;
            if (state.getBlock() instanceof ChestBlock chest) {
                Container combined = ChestBlock.getContainer(chest, state, world, pos, true);
                if (combined != null) inventory = combined;
            }
            return InventoryStorage.of(inventory, direction);
        });
        ITEM.registerForItems((stack, context) ->
                        new BridgeContainerComponentStorage(context, 27),
                Items.SHULKER_BOX, Items.WHITE_SHULKER_BOX, Items.ORANGE_SHULKER_BOX,
                Items.MAGENTA_SHULKER_BOX, Items.LIGHT_BLUE_SHULKER_BOX,
                Items.YELLOW_SHULKER_BOX, Items.LIME_SHULKER_BOX,
                Items.PINK_SHULKER_BOX, Items.GRAY_SHULKER_BOX,
                Items.LIGHT_GRAY_SHULKER_BOX, Items.CYAN_SHULKER_BOX,
                Items.PURPLE_SHULKER_BOX, Items.BLUE_SHULKER_BOX,
                Items.BROWN_SHULKER_BOX, Items.GREEN_SHULKER_BOX,
                Items.RED_SHULKER_BOX, Items.BLACK_SHULKER_BOX);
        ITEM.registerForItems((stack, context) ->
                new BridgeBundleContentsStorage(context), Items.BUNDLE);
    }

    private ItemStorage() {
    }
}
