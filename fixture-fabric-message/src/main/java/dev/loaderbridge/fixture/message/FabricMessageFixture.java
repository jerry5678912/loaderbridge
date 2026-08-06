package dev.loaderbridge.fixture.message;

import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.chat.Component;

/** Exercises server message decoration, allow/cancel, notification, and commands. */
public final class FabricMessageFixture implements ModInitializer {
    @Override public void onInitialize() {
        AtomicInteger allowedGame = new AtomicInteger();
        AtomicInteger blockedGame = new AtomicInteger();
        AtomicInteger commands = new AtomicInteger();

        ServerMessageDecoratorEvent.EVENT.register(ServerMessageDecoratorEvent.CONTENT_PHASE,
                (sender, message) -> message.copy().append("-content"));
        ServerMessageDecoratorEvent.EVENT.register(
                (sender, message) -> message.copy().append("-default"));
        ServerMessageDecoratorEvent.EVENT.register(ServerMessageDecoratorEvent.STYLING_PHASE,
                (sender, message) -> message.copy().append("-styling"));
        ServerMessageEvents.ALLOW_GAME_MESSAGE.register((server, message, overlay) -> {
            if (message.getString().equals("loaderbridge-blocked")) {
                blockedGame.incrementAndGet();
                return false;
            }
            return true;
        });
        ServerMessageEvents.GAME_MESSAGE.register((server, message, overlay) -> {
            if (message.getString().equals("loaderbridge-allowed")) allowedGame.incrementAndGet();
        });
        ServerMessageEvents.ALLOW_COMMAND_MESSAGE.register((message, source, parameters) -> true);
        ServerMessageEvents.COMMAND_MESSAGE.register((message, source, parameters) ->
                commands.incrementAndGet());

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            String decorated = server.getChatDecorator()
                    .decorate(null, Component.literal("base")).getString();
            if (!decorated.equals("base-content-default-styling")) {
                throw new IllegalStateException("LOADERBRIDGE_FABRIC_MESSAGE_DECORATOR_FAILED: "
                        + decorated);
            }
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("loaderbridge-allowed"), false);
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("loaderbridge-blocked"), false);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                    "say loaderbridge-message-command");
            if (allowedGame.get() != 1 || blockedGame.get() != 1 || commands.get() != 1) {
                throw new IllegalStateException("LOADERBRIDGE_FABRIC_MESSAGE_EVENTS_FAILED: "
                        + allowedGame + "," + blockedGame + "," + commands);
            }
            System.out.println("LOADERBRIDGE_FABRIC_MESSAGE_SERVER_READY "
                    + "decorator=content-default-styling,game=allowed-blocked,command=1");
        });
    }
}
