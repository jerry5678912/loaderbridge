package dev.loaderbridge.fabric.api.content.registry.mixin;

import dev.loaderbridge.fabric.api.content.registry.BridgeContentRegistries;
import java.util.Optional;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HoneycombItem.class)
abstract class HoneycombItemMixin {
    @Inject(method = "getWaxed", at = @At("HEAD"), cancellable = true)
    private static void loaderbridge$waxed(BlockState state,
            CallbackInfoReturnable<Optional<BlockState>> callback) {
        Optional<BlockState> waxed = BridgeContentRegistries.waxed(state);
        if (waxed.isPresent()) callback.setReturnValue(waxed);
    }
}
