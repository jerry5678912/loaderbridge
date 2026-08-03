package net.fabricmc.fabric.api.transfer.v1.item;

import com.mojang.serialization.Codec;
import java.util.Objects;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/** Immutable item and component association without a stack count. */
public interface ItemVariant extends TransferVariant<Item> {
    Codec<ItemVariant> CODEC = ItemStack.OPTIONAL_CODEC.xmap(ItemVariant::of, ItemVariant::toStack);
    StreamCodec<RegistryFriendlyByteBuf, ItemVariant> PACKET_CODEC =
            ItemStack.OPTIONAL_STREAM_CODEC.map(ItemVariant::of, ItemVariant::toStack);

    static ItemVariant blank() {
        return of(Items.AIR);
    }

    static ItemVariant of(ItemStack stack) {
        return of(stack.getItem(), stack.getComponentsPatch());
    }

    static ItemVariant of(ItemLike item) {
        return of(item, DataComponentPatch.EMPTY);
    }

    static ItemVariant of(ItemLike item, DataComponentPatch components) {
        return new BridgeItemVariant(item.asItem(), components);
    }

    default boolean matches(ItemStack stack) {
        return isOf(stack.getItem())
                && Objects.equals(stack.getComponentsPatch(), getComponents());
    }

    default Item getItem() {
        return getObject();
    }

    @SuppressWarnings("deprecation")
    default Holder<Item> getRegistryEntry() {
        return getItem().builtInRegistryHolder();
    }

    default ItemStack toStack() {
        return toStack(1);
    }

    default ItemStack toStack(int count) {
        return isBlank() ? ItemStack.EMPTY
                : new ItemStack(getRegistryEntry(), count, getComponents());
    }

    @Override
    ItemVariant withComponentChanges(DataComponentPatch changes);
}
