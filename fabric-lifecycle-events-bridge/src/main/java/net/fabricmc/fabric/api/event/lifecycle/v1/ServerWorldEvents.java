package net.fabricmc.fabric.api.event.lifecycle.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/** Fabric lifecycle API 2.6.0 server-world contract. */
public final class ServerWorldEvents {
    public static final Event<Load> LOAD = EventFactory.createArrayBacked(Load.class,
            callbacks -> (server, world) -> {
                for (Load callback : callbacks) callback.onWorldLoad(server, world);
            });
    public static final Event<Unload> UNLOAD = EventFactory.createArrayBacked(Unload.class,
            callbacks -> (server, world) -> {
                for (Unload callback : callbacks) callback.onWorldUnload(server, world);
            });

    private ServerWorldEvents() {}

    @FunctionalInterface
    public interface Load {
        void onWorldLoad(MinecraftServer server, ServerLevel world);
    }

    @FunctionalInterface
    public interface Unload {
        void onWorldUnload(MinecraftServer server, ServerLevel world);
    }
}
