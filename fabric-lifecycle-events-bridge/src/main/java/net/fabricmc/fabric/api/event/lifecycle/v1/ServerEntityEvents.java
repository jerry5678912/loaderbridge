package net.fabricmc.fabric.api.event.lifecycle.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Fabric lifecycle API 2.6.0 server-entity contract. */
public final class ServerEntityEvents {
    public static final Event<Load> ENTITY_LOAD = EventFactory.createArrayBacked(Load.class,
            callbacks -> (entity, world) -> {
                for (Load callback : callbacks) callback.onLoad(entity, world);
            });
    public static final Event<Unload> ENTITY_UNLOAD = EventFactory.createArrayBacked(Unload.class,
            callbacks -> (entity, world) -> {
                for (Unload callback : callbacks) callback.onUnload(entity, world);
            });
    public static final Event<EquipmentChange> EQUIPMENT_CHANGE = EventFactory.createArrayBacked(
            EquipmentChange.class, callbacks -> (entity, slot, previous, current) -> {
                for (EquipmentChange callback : callbacks) {
                    callback.onChange(entity, slot, previous, current);
                }
            });

    private ServerEntityEvents() {}

    @FunctionalInterface public interface Load { void onLoad(Entity entity, ServerLevel world); }
    @FunctionalInterface public interface Unload { void onUnload(Entity entity, ServerLevel world); }
    @FunctionalInterface public interface EquipmentChange {
        void onChange(LivingEntity livingEntity, EquipmentSlot equipmentSlot,
                ItemStack previousStack, ItemStack currentStack);
    }
}
