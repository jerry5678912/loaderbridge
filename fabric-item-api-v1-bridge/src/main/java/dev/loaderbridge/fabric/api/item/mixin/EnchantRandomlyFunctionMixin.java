package dev.loaderbridge.fabric.api.item.mixin;

import dev.loaderbridge.fabric.api.item.EnchantingBridge;
import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnchantRandomlyFunction.class)
public abstract class EnchantRandomlyFunctionMixin {
    @Redirect(method = "lambda$run$4",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/Enchantment;"
                            + "canEnchant(Lnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean loaderbridge$useFabricAcceptableCheck(
            Enchantment ignoredEnchantment, ItemStack stack,
            boolean ignoredOnlyCompatible, ItemStack ignoredStack,
            Holder<Enchantment> enchantment) {
        return EnchantingBridge.canEnchant(
                enchantment, stack, EnchantingContext.ACCEPTABLE);
    }
}
