package net.fabricmc.fabric.api.item.v1;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface EquipmentSlotProvider {
    EquipmentSlot getPreferredEquipmentSlot(LivingEntity entity, ItemStack stack);
}
