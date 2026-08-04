package net.fabricmc.fabric.api.entity.event.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public final class EntityElytraEvents {
    public static final Event<Allow> ALLOW = EventFactory.createArrayBacked(Allow.class,
            listeners -> entity -> {
                for (Allow listener : listeners) {
                    if (!listener.allowElytraFlight(entity)) return false;
                }
                return true;
            });
    public static final Event<Custom> CUSTOM = EventFactory.createArrayBacked(Custom.class,
            listeners -> (entity, tick) -> {
                for (Custom listener : listeners) {
                    if (listener.useCustomElytra(entity, tick)) return true;
                }
                return false;
            });

    static {
        CUSTOM.register((entity, tick) -> {
            var chest = entity.getItemBySlot(EquipmentSlot.CHEST);
            return chest.getItem() instanceof FabricElytraItem item
                    && item.useCustomElytra(entity, chest, tick);
        });
    }

    @FunctionalInterface public interface Allow {
        boolean allowElytraFlight(LivingEntity entity);
    }
    @FunctionalInterface public interface Custom {
        boolean useCustomElytra(LivingEntity entity, boolean tickElytra);
    }

    private EntityElytraEvents() { }
}
