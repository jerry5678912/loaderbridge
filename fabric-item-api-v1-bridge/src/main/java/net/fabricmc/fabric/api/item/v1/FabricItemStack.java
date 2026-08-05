package net.fabricmc.fabric.api.item.v1;

import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

public interface FabricItemStack {
    default ItemStack getRecipeRemainder() {
        ItemStack self = (ItemStack) (Object) this;
        return ((FabricItem) self.getItem()).getRecipeRemainder(self);
    }

    default boolean canBeEnchantedWith(Holder<Enchantment> enchantment,
            EnchantingContext context) {
        ItemStack self = (ItemStack) (Object) this;
        TriState result = EnchantmentEvents.ALLOW_ENCHANTING.invoker()
                .allowEnchanting(enchantment, self, context);
        return result.orElseGet(() -> ((FabricItem) self.getItem())
                .canBeEnchantedWith(self, enchantment, context));
    }

    default String getCreatorNamespace() {
        ItemStack self = (ItemStack) (Object) this;
        return ((FabricItem) self.getItem()).getCreatorNamespace(self);
    }
}
