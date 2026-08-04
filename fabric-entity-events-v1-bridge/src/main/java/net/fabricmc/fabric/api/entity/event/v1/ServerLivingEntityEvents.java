package net.fabricmc.fabric.api.entity.event.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public final class ServerLivingEntityEvents {
    public static final Event<AllowDamage> ALLOW_DAMAGE = EventFactory.createArrayBacked(
            AllowDamage.class, listeners -> (entity, source, amount) -> {
                for (AllowDamage listener : listeners) {
                    if (!listener.allowDamage(entity, source, amount)) return false;
                }
                return true;
            });
    public static final Event<AfterDamage> AFTER_DAMAGE = EventFactory.createArrayBacked(
            AfterDamage.class, listeners -> (entity, source, base, taken, blocked) -> {
                for (AfterDamage listener : listeners) {
                    listener.afterDamage(entity, source, base, taken, blocked);
                }
            });
    public static final Event<AllowDeath> ALLOW_DEATH = EventFactory.createArrayBacked(
            AllowDeath.class, listeners -> (entity, source, amount) -> {
                for (AllowDeath listener : listeners) {
                    if (!listener.allowDeath(entity, source, amount)) return false;
                }
                return true;
            });
    public static final Event<AfterDeath> AFTER_DEATH = EventFactory.createArrayBacked(
            AfterDeath.class, listeners -> (entity, source) -> {
                for (AfterDeath listener : listeners) listener.afterDeath(entity, source);
            });
    public static final Event<MobConversion> MOB_CONVERSION = EventFactory.createArrayBacked(
            MobConversion.class, listeners -> (previous, converted, keepEquipment) -> {
                for (MobConversion listener : listeners) {
                    listener.onConversion(previous, converted, keepEquipment);
                }
            });

    @FunctionalInterface public interface AllowDamage {
        boolean allowDamage(LivingEntity entity, DamageSource source, float amount);
    }
    @FunctionalInterface public interface AfterDamage {
        void afterDamage(LivingEntity entity, DamageSource source, float baseDamageTaken,
                float damageTaken, boolean blocked);
    }
    @FunctionalInterface public interface AllowDeath {
        boolean allowDeath(LivingEntity entity, DamageSource source, float amount);
    }
    @FunctionalInterface public interface AfterDeath {
        void afterDeath(LivingEntity entity, DamageSource source);
    }
    @FunctionalInterface public interface MobConversion {
        void onConversion(Mob previous, Mob converted, boolean keepEquipment);
    }

    private ServerLivingEntityEvents() { }
}
