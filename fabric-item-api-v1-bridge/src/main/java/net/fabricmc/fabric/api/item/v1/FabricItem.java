package net.fabricmc.fabric.api.item.v1;

import net.fabricmc.fabric.impl.item.FabricItemInternals;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

public interface FabricItem {
    default boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand,
            ItemStack oldStack, ItemStack newStack) { return true; }

    default boolean allowContinuingBlockBreaking(Player player,
            ItemStack oldStack, ItemStack newStack) { return false; }

    @SuppressWarnings("deprecation")
    default ItemStack getRecipeRemainder(ItemStack stack) {
        Item item = (Item) this;
        return item.hasCraftingRemainingItem()
                ? item.getCraftingRemainingItem().getDefaultInstance() : ItemStack.EMPTY;
    }

    default boolean canBeEnchantedWith(ItemStack stack, Holder<Enchantment> enchantment,
            EnchantingContext context) {
        return context == EnchantingContext.PRIMARY
                ? enchantment.value().isPrimaryItem(stack)
                : enchantment.value().isSupportedItem(stack);
    }

    default String getCreatorNamespace(ItemStack stack) {
        return stack.getItemHolder().unwrapKey().orElseThrow().location().getNamespace();
    }

    interface Settings {
        default Item.Properties equipmentSlot(EquipmentSlotProvider provider) {
            FabricItemInternals.computeExtraData((Item.Properties) this).equipmentSlot(provider);
            return (Item.Properties) this;
        }

        default Item.Properties customDamage(CustomDamageHandler handler) {
            FabricItemInternals.computeExtraData((Item.Properties) this).customDamage(handler);
            return (Item.Properties) this;
        }
    }
}
