package net.fabricmc.fabric.api.networking.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class EntityTrackingEvents {
    public static final Event<StartTracking> START_TRACKING = EventFactory.createArrayBacked(
            StartTracking.class, callbacks -> (entity, player) -> {
                for (StartTracking callback : callbacks) callback.onStartTracking(entity, player);
            });
    public static final Event<StopTracking> STOP_TRACKING = EventFactory.createArrayBacked(
            StopTracking.class, callbacks -> (entity, player) -> {
                for (StopTracking callback : callbacks) callback.onStopTracking(entity, player);
            });

    @FunctionalInterface public interface StartTracking {
        void onStartTracking(Entity trackedEntity, ServerPlayer player);
    }
    @FunctionalInterface public interface StopTracking {
        void onStopTracking(Entity trackedEntity, ServerPlayer player);
    }
    private EntityTrackingEvents() { }
}
