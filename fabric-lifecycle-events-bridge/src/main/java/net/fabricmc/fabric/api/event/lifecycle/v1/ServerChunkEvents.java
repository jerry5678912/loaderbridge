package net.fabricmc.fabric.api.event.lifecycle.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

/** Binary-compatible Fabric server chunk lifecycle events for Minecraft 1.21.1. */
public final class ServerChunkEvents {
    public static final Event<Load> CHUNK_LOAD = EventFactory.createArrayBacked(Load.class,
            callbacks -> (world, chunk) -> {
                for (Load callback : callbacks) {
                    callback.onChunkLoad(world, chunk);
                }
            });
    public static final Event<Generate> CHUNK_GENERATE = EventFactory.createArrayBacked(Generate.class,
            callbacks -> (world, chunk) -> {
                for (Generate callback : callbacks) {
                    callback.onChunkGenerate(world, chunk);
                }
            });
    public static final Event<Unload> CHUNK_UNLOAD = EventFactory.createArrayBacked(Unload.class,
            callbacks -> (world, chunk) -> {
                for (Unload callback : callbacks) {
                    callback.onChunkUnload(world, chunk);
                }
            });
    public static final Event<LevelTypeChange> CHUNK_LEVEL_TYPE_CHANGE = EventFactory.createArrayBacked(
            LevelTypeChange.class,
            (world, chunk, previous, current) -> { },
            callbacks -> (world, chunk, previous, current) -> {
                for (LevelTypeChange callback : callbacks) {
                    callback.onChunkLevelTypeChange(world, chunk, previous, current);
                }
            });

    private ServerChunkEvents() {
    }

    @FunctionalInterface
    public interface Load {
        void onChunkLoad(ServerLevel world, LevelChunk chunk);
    }

    @FunctionalInterface
    public interface Generate {
        void onChunkGenerate(ServerLevel world, LevelChunk chunk);
    }

    @FunctionalInterface
    public interface Unload {
        void onChunkUnload(ServerLevel world, LevelChunk chunk);
    }

    @FunctionalInterface
    public interface LevelTypeChange {
        void onChunkLevelTypeChange(ServerLevel world, LevelChunk chunk,
                FullChunkStatus oldLevelType, FullChunkStatus newLevelType);
    }
}
