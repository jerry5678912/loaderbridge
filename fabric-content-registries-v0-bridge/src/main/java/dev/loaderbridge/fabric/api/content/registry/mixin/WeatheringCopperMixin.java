package dev.loaderbridge.fabric.api.content.registry.mixin;

import dev.loaderbridge.fabric.api.content.registry.BridgeContentRegistries;
import java.util.Optional;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WeatheringCopper.class)
interface WeatheringCopperMixin {
    @Inject(method = "getNext(Lnet/minecraft/world/level/block/Block;)Ljava/util/Optional;",
            at = @At("HEAD"), cancellable = true)
    private static void loaderbridge$next(Block block,
            CallbackInfoReturnable<Optional<Block>> callback) {
        Optional<Block> next = BridgeContentRegistries.next(block);
        if (next.isPresent()) callback.setReturnValue(next);
    }

    @Inject(method = "getPrevious(Lnet/minecraft/world/level/block/Block;)Ljava/util/Optional;",
            at = @At("HEAD"), cancellable = true)
    private static void loaderbridge$previous(Block block,
            CallbackInfoReturnable<Optional<Block>> callback) {
        Optional<Block> previous = BridgeContentRegistries.previous(block);
        if (previous.isPresent()) callback.setReturnValue(previous);
    }

    @Inject(method = "getFirst(Lnet/minecraft/world/level/block/Block;)Lnet/minecraft/world/level/block/Block;",
            at = @At("HEAD"), cancellable = true)
    private static void loaderbridge$first(Block block,
            CallbackInfoReturnable<Block> callback) {
        BridgeContentRegistries.first(block).ifPresent(callback::setReturnValue);
    }
}
