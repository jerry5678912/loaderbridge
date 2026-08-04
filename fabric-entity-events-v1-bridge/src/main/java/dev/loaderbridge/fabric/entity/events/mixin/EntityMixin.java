package dev.loaderbridge.fabric.entity.events.mixin;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow private Level level;

    @Inject(method = "changeDimension", at = @At("RETURN"))
    private void loaderbridge$afterDimensionChange(DimensionTransition transition,
            CallbackInfoReturnable<Entity> callback) {
        Entity replacement = callback.getReturnValue();
        Entity original = (Entity) (Object) this;
        if (!(original instanceof net.minecraft.server.level.ServerPlayer)
                && replacement != null && level instanceof ServerLevel origin
                && replacement.level() instanceof ServerLevel destination
                && origin != destination) {
            ServerEntityWorldChangeEvents.AFTER_ENTITY_CHANGE_WORLD.invoker()
                    .afterChangeWorld(original, replacement, origin, destination);
        }
    }
}
