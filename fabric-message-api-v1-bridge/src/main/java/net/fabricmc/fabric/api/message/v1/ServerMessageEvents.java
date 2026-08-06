package net.fabricmc.fabric.api.message.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Binary-compatible server message events from Fabric Message API v1. */
public final class ServerMessageEvents {
    public static final Event<AllowChatMessage> ALLOW_CHAT_MESSAGE =
            EventFactory.createArrayBacked(AllowChatMessage.class,
                    listeners -> (message, sender, parameters) -> {
                        for (AllowChatMessage listener : listeners) {
                            if (!listener.allowChatMessage(message, sender, parameters)) return false;
                        }
                        return true;
                    });
    public static final Event<AllowGameMessage> ALLOW_GAME_MESSAGE =
            EventFactory.createArrayBacked(AllowGameMessage.class,
                    listeners -> (server, message, overlay) -> {
                        for (AllowGameMessage listener : listeners) {
                            if (!listener.allowGameMessage(server, message, overlay)) return false;
                        }
                        return true;
                    });
    public static final Event<AllowCommandMessage> ALLOW_COMMAND_MESSAGE =
            EventFactory.createArrayBacked(AllowCommandMessage.class,
                    listeners -> (message, source, parameters) -> {
                        for (AllowCommandMessage listener : listeners) {
                            if (!listener.allowCommandMessage(message, source, parameters)) return false;
                        }
                        return true;
                    });
    public static final Event<ChatMessage> CHAT_MESSAGE =
            EventFactory.createArrayBacked(ChatMessage.class,
                    listeners -> (message, sender, parameters) -> {
                        for (ChatMessage listener : listeners) {
                            listener.onChatMessage(message, sender, parameters);
                        }
                    });
    public static final Event<GameMessage> GAME_MESSAGE =
            EventFactory.createArrayBacked(GameMessage.class,
                    listeners -> (server, message, overlay) -> {
                        for (GameMessage listener : listeners) {
                            listener.onGameMessage(server, message, overlay);
                        }
                    });
    public static final Event<CommandMessage> COMMAND_MESSAGE =
            EventFactory.createArrayBacked(CommandMessage.class,
                    listeners -> (message, source, parameters) -> {
                        for (CommandMessage listener : listeners) {
                            listener.onCommandMessage(message, source, parameters);
                        }
                    });

    private ServerMessageEvents() {}

    @FunctionalInterface
    public interface AllowChatMessage {
        boolean allowChatMessage(PlayerChatMessage message, ServerPlayer sender,
                ChatType.Bound parameters);
    }

    @FunctionalInterface
    public interface AllowGameMessage {
        boolean allowGameMessage(MinecraftServer server, Component message, boolean overlay);
    }

    @FunctionalInterface
    public interface AllowCommandMessage {
        boolean allowCommandMessage(PlayerChatMessage message, CommandSourceStack source,
                ChatType.Bound parameters);
    }

    @FunctionalInterface
    public interface ChatMessage {
        void onChatMessage(PlayerChatMessage message, ServerPlayer sender,
                ChatType.Bound parameters);
    }

    @FunctionalInterface
    public interface GameMessage {
        void onGameMessage(MinecraftServer server, Component message, boolean overlay);
    }

    @FunctionalInterface
    public interface CommandMessage {
        void onCommandMessage(PlayerChatMessage message, CommandSourceStack source,
                ChatType.Bound parameters);
    }
}
