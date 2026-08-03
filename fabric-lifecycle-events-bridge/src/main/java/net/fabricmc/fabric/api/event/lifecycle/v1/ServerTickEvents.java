package net.fabricmc.fabric.api.event.lifecycle.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/** Fabric lifecycle API 2.6.0 server tick contract. */
public final class ServerTickEvents {
    public static final Event<StartTick> START_SERVER_TICK = EventFactory.createArrayBacked(
            StartTick.class, callbacks -> server -> {
                for (StartTick callback : callbacks) callback.onStartTick(server);
            });
    public static final Event<EndTick> END_SERVER_TICK = EventFactory.createArrayBacked(
            EndTick.class, callbacks -> server -> {
                for (EndTick callback : callbacks) callback.onEndTick(server);
            });
    public static final Event<StartWorldTick> START_WORLD_TICK = EventFactory.createArrayBacked(
            StartWorldTick.class, callbacks -> world -> {
                for (StartWorldTick callback : callbacks) callback.onStartTick(world);
            });
    public static final Event<EndWorldTick> END_WORLD_TICK = EventFactory.createArrayBacked(
            EndWorldTick.class, callbacks -> world -> {
                for (EndWorldTick callback : callbacks) callback.onEndTick(world);
            });

    private ServerTickEvents() {}

    @FunctionalInterface
    public interface StartTick {
        void onStartTick(MinecraftServer server);
    }

    @FunctionalInterface
    public interface EndTick {
        void onEndTick(MinecraftServer server);
    }

    @FunctionalInterface
    public interface StartWorldTick {
        void onStartTick(ServerLevel world);
    }

    @FunctionalInterface
    public interface EndWorldTick {
        void onEndTick(ServerLevel world);
    }
}
