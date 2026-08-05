package net.fabricmc.fabric.api.event.player;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class PlayerBlockBreakEvents {
    public static final Event<Before> BEFORE = EventFactory.createArrayBacked(
            Before.class, listeners -> (world, player, pos, state, entity) -> {
                for (Before listener : listeners) {
                    if (!listener.beforeBlockBreak(world, player, pos, state, entity)) return false;
                }
                return true;
            });
    public static final Event<After> AFTER = EventFactory.createArrayBacked(
            After.class, listeners -> (world, player, pos, state, entity) -> {
                for (After listener : listeners) {
                    listener.afterBlockBreak(world, player, pos, state, entity);
                }
            });
    public static final Event<Canceled> CANCELED = EventFactory.createArrayBacked(
            Canceled.class, listeners -> (world, player, pos, state, entity) -> {
                for (Canceled listener : listeners) {
                    listener.onBlockBreakCanceled(world, player, pos, state, entity);
                }
            });

    private PlayerBlockBreakEvents() {
    }

    @FunctionalInterface
    public interface Before {
        boolean beforeBlockBreak(Level world, Player player, BlockPos pos,
                BlockState state, @Nullable BlockEntity blockEntity);
    }

    @FunctionalInterface
    public interface After {
        void afterBlockBreak(Level world, Player player, BlockPos pos,
                BlockState state, @Nullable BlockEntity blockEntity);
    }

    @FunctionalInterface
    public interface Canceled {
        void onBlockBreakCanceled(Level world, Player player, BlockPos pos,
                BlockState state, @Nullable BlockEntity blockEntity);
    }
}
