package net.fabricmc.fabric.api.item.v1;

import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.impl.item.FabricItemInternals;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TippedArrowItem;
import net.minecraft.world.item.alchemy.Potion;
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
                ? ((Item) this).canApplyAtEnchantingTable(stack, enchantment.value())
                : enchantment.value().isSupportedItem(stack);
    }

    default String getCreatorNamespace(ItemStack stack) {
        Holder<?> entry = stack.getItemHolder();
        if ((this instanceof PotionItem || this instanceof TippedArrowItem)
                && stack.has(DataComponents.POTION_CONTENTS)) {
            Optional<Holder<Potion>> potion = stack.get(DataComponents.POTION_CONTENTS).potion();
            if (potion.isPresent()) entry = potion.get();
        } else if (stack.is(Items.ENCHANTED_BOOK)
                && stack.has(DataComponents.STORED_ENCHANTMENTS)) {
            Set<Holder<Enchantment>> enchantments = stack.get(
                    DataComponents.STORED_ENCHANTMENTS).keySet();
            if (enchantments.size() == 1) entry = enchantments.iterator().next();
        }
        return entry.unwrapKey().orElseThrow().location().getNamespace();
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
