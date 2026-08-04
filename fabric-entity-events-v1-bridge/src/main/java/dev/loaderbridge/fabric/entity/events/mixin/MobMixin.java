package dev.loaderbridge.fabric.entity.events.mixin;

import dev.loaderbridge.fabric.entity.events.FabricEntityEventsV1BridgeMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Mob.class)
public abstract class MobMixin {
    @Redirect(method = "convertTo", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;"
                    + "addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean loaderbridge$beforeConvertedEntitySpawn(Level level, Entity converted,
            EntityType<?> targetType, boolean keepEquipment) {
        FabricEntityEventsV1BridgeMod.onMixinMobConversion(
                (Mob) (Object) this, (Mob) converted, keepEquipment);
        return level.addFreshEntity(converted);
    }
}
