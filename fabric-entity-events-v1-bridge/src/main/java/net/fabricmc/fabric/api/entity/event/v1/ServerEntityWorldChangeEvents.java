package net.fabricmc.fabric.api.entity.event.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class ServerEntityWorldChangeEvents {
    public static final Event<AfterEntityChange> AFTER_ENTITY_CHANGE_WORLD =
            EventFactory.createArrayBacked(AfterEntityChange.class, listeners ->
                    (original, replacement, origin, destination) -> {
                        for (AfterEntityChange listener : listeners) {
                            listener.afterChangeWorld(original, replacement, origin, destination);
                        }
                    });
    public static final Event<AfterPlayerChange> AFTER_PLAYER_CHANGE_WORLD =
            EventFactory.createArrayBacked(AfterPlayerChange.class, listeners ->
                    (player, origin, destination) -> {
                        for (AfterPlayerChange listener : listeners) {
                            listener.afterChangeWorld(player, origin, destination);
                        }
                    });

    @FunctionalInterface public interface AfterEntityChange {
        void afterChangeWorld(Entity originalEntity, Entity newEntity,
                ServerLevel origin, ServerLevel destination);
    }
    @FunctionalInterface public interface AfterPlayerChange {
        void afterChangeWorld(ServerPlayer player, ServerLevel origin, ServerLevel destination);
    }

    private ServerEntityWorldChangeEvents() { }
}
