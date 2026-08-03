package net.fabricmc.fabric.api.event.lifecycle.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.CloseableResourceManager;

/** Fabric lifecycle API 2.6.0 server lifecycle contract. */
public final class ServerLifecycleEvents {
    public static final Event<ServerStarting> SERVER_STARTING = event(ServerStarting.class,
            callbacks -> server -> each(callbacks, callback -> callback.onServerStarting(server)));
    public static final Event<ServerStarted> SERVER_STARTED = event(ServerStarted.class,
            callbacks -> server -> each(callbacks, callback -> callback.onServerStarted(server)));
    public static final Event<ServerStopping> SERVER_STOPPING = event(ServerStopping.class,
            callbacks -> server -> each(callbacks, callback -> callback.onServerStopping(server)));
    public static final Event<ServerStopped> SERVER_STOPPED = event(ServerStopped.class,
            callbacks -> server -> each(callbacks, callback -> callback.onServerStopped(server)));
    public static final Event<SyncDataPackContents> SYNC_DATA_PACK_CONTENTS = event(
            SyncDataPackContents.class, callbacks -> (player, joined) ->
                    each(callbacks, callback -> callback.onSyncDataPackContents(player, joined)));
    public static final Event<StartDataPackReload> START_DATA_PACK_RELOAD = event(
            StartDataPackReload.class, callbacks -> (server, resources) ->
                    each(callbacks, callback -> callback.startDataPackReload(server, resources)));
    public static final Event<EndDataPackReload> END_DATA_PACK_RELOAD = event(
            EndDataPackReload.class, callbacks -> (server, resources, success) ->
                    each(callbacks, callback -> callback.endDataPackReload(server, resources, success)));
    public static final Event<BeforeSave> BEFORE_SAVE = event(BeforeSave.class,
            callbacks -> (server, flush, force) ->
                    each(callbacks, callback -> callback.onBeforeSave(server, flush, force)));
    public static final Event<AfterSave> AFTER_SAVE = event(AfterSave.class,
            callbacks -> (server, flush, force) ->
                    each(callbacks, callback -> callback.onAfterSave(server, flush, force)));

    private ServerLifecycleEvents() {}

    private static <T> Event<T> event(Class<T> type, java.util.function.Function<T[], T> invokerFactory) {
        return EventFactory.createArrayBacked(type, invokerFactory);
    }

    private static <T> void each(T[] callbacks, java.util.function.Consumer<T> invocation) {
        for (T callback : callbacks) {
            invocation.accept(callback);
        }
    }

    @FunctionalInterface public interface ServerStarting { void onServerStarting(MinecraftServer server); }
    @FunctionalInterface public interface ServerStarted { void onServerStarted(MinecraftServer server); }
    @FunctionalInterface public interface ServerStopping { void onServerStopping(MinecraftServer server); }
    @FunctionalInterface public interface ServerStopped { void onServerStopped(MinecraftServer server); }
    @FunctionalInterface public interface SyncDataPackContents {
        void onSyncDataPackContents(ServerPlayer player, boolean joined);
    }
    @FunctionalInterface public interface StartDataPackReload {
        void startDataPackReload(MinecraftServer server, CloseableResourceManager resourceManager);
    }
    @FunctionalInterface public interface EndDataPackReload {
        void endDataPackReload(MinecraftServer server, CloseableResourceManager resourceManager,
                boolean success);
    }
    @FunctionalInterface public interface BeforeSave {
        void onBeforeSave(MinecraftServer server, boolean flush, boolean force);
    }
    @FunctionalInterface public interface AfterSave {
        void onAfterSave(MinecraftServer server, boolean flush, boolean force);
    }
}
