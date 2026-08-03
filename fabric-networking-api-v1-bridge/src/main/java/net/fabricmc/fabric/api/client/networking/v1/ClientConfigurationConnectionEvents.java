package net.fabricmc.fabric.api.client.networking.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientConfigurationPacketListenerImpl;

public final class ClientConfigurationConnectionEvents {
    public static final Event<Init> INIT = EventFactory.createArrayBacked(
            Init.class, callbacks -> (handler, client) -> {
                for (Init callback : callbacks) callback.onConfigurationInit(handler, client);
            });
    public static final Event<Start> START = EventFactory.createArrayBacked(
            Start.class, callbacks -> (handler, client) -> {
                for (Start callback : callbacks) callback.onConfigurationStart(handler, client);
            });
    public static final Event<Complete> COMPLETE = EventFactory.createArrayBacked(
            Complete.class, callbacks -> (handler, client) -> {
                for (Complete callback : callbacks) callback.onConfigurationComplete(handler, client);
            });
    public static final Event<Disconnect> DISCONNECT = EventFactory.createArrayBacked(
            Disconnect.class, callbacks -> (handler, client) -> {
                for (Disconnect callback : callbacks) callback.onConfigurationDisconnect(handler, client);
            });

    @Deprecated
    public static final Event<Ready> READY = EventFactory.createArrayBacked(
            Ready.class, callbacks -> (handler, client) -> {
                for (Ready callback : callbacks) callback.onConfigurationReady(handler, client);
            });

    @FunctionalInterface public interface Init {
        void onConfigurationInit(ClientConfigurationPacketListenerImpl handler, Minecraft client);
    }
    @FunctionalInterface public interface Start {
        void onConfigurationStart(ClientConfigurationPacketListenerImpl handler, Minecraft client);
    }
    @FunctionalInterface public interface Complete {
        void onConfigurationComplete(ClientConfigurationPacketListenerImpl handler, Minecraft client);
    }
    @FunctionalInterface public interface Disconnect {
        void onConfigurationDisconnect(ClientConfigurationPacketListenerImpl handler, Minecraft client);
    }
    @Deprecated @FunctionalInterface public interface Ready {
        void onConfigurationReady(ClientConfigurationPacketListenerImpl handler, Minecraft client);
    }

    private ClientConfigurationConnectionEvents() { }
}
