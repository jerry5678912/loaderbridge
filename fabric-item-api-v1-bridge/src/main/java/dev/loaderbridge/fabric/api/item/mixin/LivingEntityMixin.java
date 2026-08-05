package dev.loaderbridge.fabric.api.item.mixin;

import net.fabricmc.fabric.api.item.v1.EquipmentSlotProvider;
import net.fabricmc.fabric.impl.item.ItemExtensions;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "getEquipmentSlotForItem", at = @At("HEAD"), cancellable = true)
    private void loaderbridge$fabricEquipmentSlot(
            ItemStack stack, CallbackInfoReturnable<EquipmentSlot> callback) {
        EquipmentSlotProvider provider = ((ItemExtensions) stack.getItem())
                .fabric_getEquipmentSlotProvider();
        if (provider != null) {
            callback.setReturnValue(provider.getPreferredEquipmentSlot(
                    (LivingEntity) (Object) this, stack));
        }
    }
}
