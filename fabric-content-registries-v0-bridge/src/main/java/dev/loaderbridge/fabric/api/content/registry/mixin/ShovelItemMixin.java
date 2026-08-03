package dev.loaderbridge.fabric.api.content.registry.mixin;

import dev.loaderbridge.fabric.api.content.registry.BridgeContentRegistries;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShovelItem.class)
abstract class ShovelItemMixin {
    @Inject(method = "getShovelPathingState", at = @At("HEAD"), cancellable = true)
    private static void loaderbridge$flattening(BlockState state,
            CallbackInfoReturnable<BlockState> callback) {
        BridgeContentRegistries.flattened(state).ifPresent(callback::setReturnValue);
    }
}
