package net.fabricmc.fabric.api.entity.event.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class EntitySleepEvents {
    public static final Event<AllowSleeping> ALLOW_SLEEPING = event(AllowSleeping.class,
            listeners -> (player, pos) -> {
                for (AllowSleeping listener : listeners) {
                    Player.BedSleepingProblem result = listener.allowSleep(player, pos);
                    if (result != null) return result;
                }
                return null;
            });
    public static final Event<StartSleeping> START_SLEEPING = event(StartSleeping.class,
            listeners -> (entity, pos) -> {
                for (StartSleeping listener : listeners) listener.onStartSleeping(entity, pos);
            });
    public static final Event<StopSleeping> STOP_SLEEPING = event(StopSleeping.class,
            listeners -> (entity, pos) -> {
                for (StopSleeping listener : listeners) listener.onStopSleeping(entity, pos);
            });
    public static final Event<AllowBed> ALLOW_BED = event(AllowBed.class,
            listeners -> (entity, pos, state, vanilla) -> {
                for (AllowBed listener : listeners) {
                    InteractionResult result = listener.allowBed(entity, pos, state, vanilla);
                    if (result != InteractionResult.PASS) return result;
                }
                return InteractionResult.PASS;
            });
    public static final Event<AllowSleepTime> ALLOW_SLEEP_TIME = event(AllowSleepTime.class,
            listeners -> (player, pos, vanilla) -> {
                for (AllowSleepTime listener : listeners) {
                    InteractionResult result = listener.allowSleepTime(player, pos, vanilla);
                    if (result != InteractionResult.PASS) return result;
                }
                return InteractionResult.PASS;
            });
    public static final Event<AllowNearbyMonsters> ALLOW_NEARBY_MONSTERS = event(
            AllowNearbyMonsters.class, listeners -> (player, pos, vanilla) -> {
                for (AllowNearbyMonsters listener : listeners) {
                    InteractionResult result = listener.allowNearbyMonsters(player, pos, vanilla);
                    if (result != InteractionResult.PASS) return result;
                }
                return InteractionResult.PASS;
            });
    public static final Event<AllowResettingTime> ALLOW_RESETTING_TIME = event(
            AllowResettingTime.class, listeners -> player -> {
                for (AllowResettingTime listener : listeners) {
                    if (!listener.allowResettingTime(player)) return false;
                }
                return true;
            });
    public static final Event<ModifySleepingDirection> MODIFY_SLEEPING_DIRECTION = event(
            ModifySleepingDirection.class, listeners -> (entity, pos, direction) -> {
                for (ModifySleepingDirection listener : listeners) {
                    direction = listener.modifySleepDirection(entity, pos, direction);
                }
                return direction;
            });
    public static final Event<AllowSettingSpawn> ALLOW_SETTING_SPAWN = event(
            AllowSettingSpawn.class, listeners -> (player, pos) -> {
                for (AllowSettingSpawn listener : listeners) {
                    if (!listener.allowSettingSpawn(player, pos)) return false;
                }
                return true;
            });
    public static final Event<SetBedOccupationState> SET_BED_OCCUPATION_STATE = event(
            SetBedOccupationState.class, listeners -> (entity, pos, state, occupied) -> {
                for (SetBedOccupationState listener : listeners) {
                    if (listener.setBedOccupationState(entity, pos, state, occupied)) return true;
                }
                return false;
            });
    public static final Event<ModifyWakeUpPosition> MODIFY_WAKE_UP_POSITION = event(
            ModifyWakeUpPosition.class, listeners -> (entity, pos, state, wakeUpPos) -> {
                for (ModifyWakeUpPosition listener : listeners) {
                    wakeUpPos = listener.modifyWakeUpPosition(entity, pos, state, wakeUpPos);
                }
                return wakeUpPos;
            });

    private static <T> Event<T> event(Class<T> type,
            java.util.function.Function<T[], T> factory) {
        return EventFactory.createArrayBacked(type, factory);
    }

    @FunctionalInterface public interface AllowSleeping {
        @Nullable Player.BedSleepingProblem allowSleep(Player player, BlockPos sleepingPos);
    }
    @FunctionalInterface public interface StartSleeping {
        void onStartSleeping(LivingEntity entity, BlockPos sleepingPos);
    }
    @FunctionalInterface public interface StopSleeping {
        void onStopSleeping(LivingEntity entity, BlockPos sleepingPos);
    }
    @FunctionalInterface public interface AllowBed {
        InteractionResult allowBed(LivingEntity entity, BlockPos sleepingPos,
                BlockState state, boolean vanillaResult);
    }
    @FunctionalInterface public interface AllowSleepTime {
        InteractionResult allowSleepTime(Player player, BlockPos sleepingPos,
                boolean vanillaResult);
    }
    @FunctionalInterface public interface AllowNearbyMonsters {
        InteractionResult allowNearbyMonsters(Player player, BlockPos sleepingPos,
                boolean vanillaResult);
    }
    @FunctionalInterface public interface AllowResettingTime {
        boolean allowResettingTime(Player player);
    }
    @FunctionalInterface public interface ModifySleepingDirection {
        @Nullable Direction modifySleepDirection(LivingEntity entity, BlockPos sleepingPos,
                @Nullable Direction sleepingDirection);
    }
    @FunctionalInterface public interface AllowSettingSpawn {
        boolean allowSettingSpawn(Player player, BlockPos sleepingPos);
    }
    @FunctionalInterface public interface SetBedOccupationState {
        boolean setBedOccupationState(LivingEntity entity, BlockPos sleepingPos,
                BlockState bedState, boolean occupied);
    }
    @FunctionalInterface public interface ModifyWakeUpPosition {
        @Nullable Vec3 modifyWakeUpPosition(LivingEntity entity, BlockPos sleepingPos,
                BlockState bedState, @Nullable Vec3 wakeUpPos);
    }

    private EntitySleepEvents() { }
}
