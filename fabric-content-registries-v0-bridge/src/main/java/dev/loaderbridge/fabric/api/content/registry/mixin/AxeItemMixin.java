package dev.loaderbridge.fabric.api.content.registry.mixin;

import dev.loaderbridge.fabric.api.content.registry.BridgeContentRegistries;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AxeItem.class)
abstract class AxeItemMixin {
    @Inject(method = "getAxeStrippingState", at = @At("HEAD"), cancellable = true)
    private static void loaderbridge$stripping(BlockState state,
            CallbackInfoReturnable<BlockState> callback) {
        BridgeContentRegistries.stripped(state).ifPresent(callback::setReturnValue);
    }
}
