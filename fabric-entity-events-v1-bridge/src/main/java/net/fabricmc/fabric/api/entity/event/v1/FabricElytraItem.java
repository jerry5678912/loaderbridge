package net.fabricmc.fabric.api.entity.event.v1;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;

public interface FabricElytraItem {
    default boolean useCustomElytra(LivingEntity entity, ItemStack chestStack,
            boolean tickElytra) {
        if (!ElytraItem.isFlyEnabled(chestStack)) return false;
        if (tickElytra) doVanillaElytraTick(entity, chestStack);
        return true;
    }

    default void doVanillaElytraTick(LivingEntity entity, ItemStack chestStack) {
        int nextRoll = entity.getFallFlyingTicks() + 1;
        if (!entity.level().isClientSide && nextRoll % 10 == 0) {
            if ((nextRoll / 10) % 2 == 0) {
                chestStack.hurtAndBreak(1, entity, EquipmentSlot.CHEST);
            }
            entity.gameEvent(GameEvent.ELYTRA_GLIDE);
        }
    }
}
