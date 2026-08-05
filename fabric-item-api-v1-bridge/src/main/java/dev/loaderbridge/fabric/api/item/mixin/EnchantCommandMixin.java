package dev.loaderbridge.fabric.api.item.mixin;

import java.util.Collection;
import dev.loaderbridge.fabric.api.item.EnchantingBridge;
import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.server.commands.EnchantCommand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnchantCommand.class)
public abstract class EnchantCommandMixin {
    @Redirect(method = "enchant",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/Enchantment;"
                            + "canEnchant(Lnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean loaderbridge$useFabricAcceptableCheck(
            Enchantment ignoredEnchantment, ItemStack stack,
            CommandSourceStack ignoredSource,
            Collection<? extends Entity> ignoredTargets,
            Holder<Enchantment> enchantment, int ignoredLevel) {
        return EnchantingBridge.canEnchant(
                enchantment, stack, EnchantingContext.ACCEPTABLE);
    }
}
