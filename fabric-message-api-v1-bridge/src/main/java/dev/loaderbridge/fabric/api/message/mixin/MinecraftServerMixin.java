package dev.loaderbridge.fabric.api.message.mixin;

import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "getChatDecorator", at = @At("RETURN"), cancellable = true)
    private void loaderbridge$installDecorator(CallbackInfoReturnable<ChatDecorator> callback) {
        ChatDecorator hostDecorator = callback.getReturnValue();
        callback.setReturnValue((sender, message) -> ServerMessageDecoratorEvent.EVENT.invoker()
                .decorate(sender, hostDecorator.decorate(sender, message)));
    }
}
