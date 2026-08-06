package dev.loaderbridge.fabric.api.object.builder.mixin;

import java.util.List;
import net.fabricmc.fabric.api.object.builder.v1.entity.MinecartComparatorLogic;
import net.fabricmc.fabric.api.object.builder.v1.entity.MinecartComparatorLogicRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DetectorRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DetectorRailBlock.class)
public abstract class DetectorRailBlockFabricMixin {
    @Inject(method = "getAnalogOutputSignal", at = @At("HEAD"), cancellable = true)
    private void loaderbridge$useCustomMinecartLogic(BlockState state, Level level,
            BlockPos position, CallbackInfoReturnable<Integer> callback) {
        if (!state.getValue(DetectorRailBlock.POWERED)) return;
        AABB search = new AABB(position.getX() + 0.2D, position.getY(),
                position.getZ() + 0.2D, position.getX() + 0.8D,
                position.getY() + 0.8D, position.getZ() + 0.8D);
        List<AbstractMinecart> carts = level.getEntitiesOfClass(AbstractMinecart.class, search,
                cart -> MinecartComparatorLogicRegistry.getCustomComparatorLogic(
                        cart.getType()) != null);
        for (AbstractMinecart cart : carts) {
            MinecartComparatorLogic<AbstractMinecart> logic =
                    MinecartComparatorLogicRegistry.getCustomComparatorLogic(cart.getType());
            int value = logic.getComparatorValue(cart, state, position);
            if (value >= 0) {
                callback.setReturnValue(value);
                return;
            }
        }
    }
}
