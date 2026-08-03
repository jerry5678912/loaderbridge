package net.fabricmc.fabric.api.event.lifecycle.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Fabric lifecycle API 2.6.0 server block-entity contract. */
public final class ServerBlockEntityEvents {
    public static final Event<Load> BLOCK_ENTITY_LOAD = EventFactory.createArrayBacked(Load.class,
            callbacks -> (blockEntity, world) -> {
                for (Load callback : callbacks) callback.onLoad(blockEntity, world);
            });
    public static final Event<Unload> BLOCK_ENTITY_UNLOAD = EventFactory.createArrayBacked(Unload.class,
            callbacks -> (blockEntity, world) -> {
                for (Unload callback : callbacks) callback.onUnload(blockEntity, world);
            });

    private ServerBlockEntityEvents() {}

    @FunctionalInterface public interface Load {
        void onLoad(BlockEntity blockEntity, ServerLevel world);
    }
    @FunctionalInterface public interface Unload {
        void onUnload(BlockEntity blockEntity, ServerLevel world);
    }
}
