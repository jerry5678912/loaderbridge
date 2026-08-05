package dev.loaderbridge.fabric.api.item.mixin;

import dev.loaderbridge.fabric.api.item.EnchantingBridge;
import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
    @Redirect(method = "lambda$getAvailableEnchantmentResults$41",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;"
                            + "canApplyAtEnchantingTable("
                            + "Lnet/minecraft/world/item/enchantment/Enchantment;)Z"))
    private static boolean loaderbridge$useFabricPrimaryCheck(ItemStack stack,
            Enchantment ignoredEnchantment, ItemStack ignoredStack,
            boolean ignoredBook, Holder<Enchantment> enchantment) {
        return EnchantingBridge.canEnchant(
                enchantment, stack, EnchantingContext.PRIMARY);
    }
}
