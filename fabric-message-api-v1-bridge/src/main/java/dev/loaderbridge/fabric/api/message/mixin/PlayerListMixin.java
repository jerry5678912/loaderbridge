package dev.loaderbridge.fabric.api.message.mixin;

import java.util.function.Function;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Shadow @Final private MinecraftServer server;

    @Inject(method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V",
            at = @At("HEAD"), cancellable = true)
    private void loaderbridge$chat(PlayerChatMessage message, ServerPlayer sender,
            ChatType.Bound parameters, CallbackInfo callback) {
        if (!ServerMessageEvents.ALLOW_CHAT_MESSAGE.invoker()
                .allowChatMessage(message, sender, parameters)) {
            callback.cancel();
            return;
        }
        ServerMessageEvents.CHAT_MESSAGE.invoker().onChatMessage(message, sender, parameters);
    }

    @Inject(method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Ljava/util/function/Function;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void loaderbridge$game(Component message,
            Function<ServerPlayer, Component> playerMessageFactory, boolean overlay,
            CallbackInfo callback) {
        if (!ServerMessageEvents.ALLOW_GAME_MESSAGE.invoker()
                .allowGameMessage(server, message, overlay)) {
            callback.cancel();
            return;
        }
        ServerMessageEvents.GAME_MESSAGE.invoker().onGameMessage(server, message, overlay);
    }

    @Inject(method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Lnet/minecraft/commands/CommandSourceStack;Lnet/minecraft/network/chat/ChatType$Bound;)V",
            at = @At("HEAD"), cancellable = true)
    private void loaderbridge$command(PlayerChatMessage message, CommandSourceStack source,
            ChatType.Bound parameters, CallbackInfo callback) {
        if (!ServerMessageEvents.ALLOW_COMMAND_MESSAGE.invoker()
                .allowCommandMessage(message, source, parameters)) {
            callback.cancel();
            return;
        }
        ServerMessageEvents.COMMAND_MESSAGE.invoker()
                .onCommandMessage(message, source, parameters);
    }
}
