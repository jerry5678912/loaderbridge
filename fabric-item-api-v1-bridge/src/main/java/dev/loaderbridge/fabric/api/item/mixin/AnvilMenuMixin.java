package dev.loaderbridge.fabric.api.item.mixin;

import dev.loaderbridge.fabric.api.item.EnchantingBridge;
import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.minecraft.core.Holder;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
    @Redirect(method = "createResult",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Holder;"
                    + "value()Ljava/lang/Object;"))
    @SuppressWarnings("unchecked")
    private Object loaderbridge$captureEnchantment(Holder<?> holder) {
        Object value = holder.value();
        if (value instanceof Enchantment) {
            EnchantingBridge.capture((Holder<Enchantment>) holder);
        }
        return value;
    }

    @Redirect(method = "createResult",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/Enchantment;"
                            + "canEnchant(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean loaderbridge$useFabricAcceptableCheck(
            Enchantment enchantment, ItemStack stack) {
        Holder<Enchantment> captured = EnchantingBridge.consumeCapture();
        return captured == null ? enchantment.canEnchant(stack)
                : EnchantingBridge.canEnchant(
                        captured, stack, EnchantingContext.ACCEPTABLE);
    }
}
