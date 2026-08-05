package dev.loaderbridge.fabric.api.item.mixin;

import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.item.v1.CustomDamageHandler;
import net.fabricmc.fabric.api.item.v1.FabricItemStack;
import net.fabricmc.fabric.impl.item.ItemExtensions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements FabricItemStack {
    @Inject(method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V",
            at = @At("HEAD"), cancellable = true)
    private void loaderbridge$fabricDamage(int amount, LivingEntity entity,
            EquipmentSlot slot, CallbackInfo callback) {
        ItemStack self = (ItemStack) (Object) this;
        CustomDamageHandler handler = ((ItemExtensions) self.getItem()).fabric_getCustomDamageHandler();
        if (handler == null || entity.level().isClientSide || entity instanceof net.minecraft.world.entity.player.Player player
                && player.getAbilities().instabuild) return;

        AtomicBoolean broken = new AtomicBoolean();
        int adjusted = handler.damage(self, amount, entity, slot, () -> {
            broken.set(true);
            self.shrink(1);
            entity.onEquippedItemBroken(self.getItem(), slot);
        });
        if (!broken.get() && entity.level() instanceof ServerLevel serverLevel) {
            self.hurtAndBreak(adjusted, serverLevel,
                    entity instanceof ServerPlayer serverPlayer ? serverPlayer : null,
                    item -> entity.onEquippedItemBroken(item, slot));
        }
        callback.cancel();
    }
}
