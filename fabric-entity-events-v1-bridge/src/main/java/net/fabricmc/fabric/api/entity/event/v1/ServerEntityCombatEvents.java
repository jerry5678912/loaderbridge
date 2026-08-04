package net.fabricmc.fabric.api.entity.event.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class ServerEntityCombatEvents {
    public static final Event<AfterKilledOtherEntity> AFTER_KILLED_OTHER_ENTITY =
            EventFactory.createArrayBacked(AfterKilledOtherEntity.class, listeners ->
                    (world, entity, killed) -> {
                        for (AfterKilledOtherEntity listener : listeners) {
                            listener.afterKilledOtherEntity(world, entity, killed);
                        }
                    });

    @FunctionalInterface public interface AfterKilledOtherEntity {
        void afterKilledOtherEntity(ServerLevel world, Entity entity, LivingEntity killedEntity);
    }

    private ServerEntityCombatEvents() { }
}
