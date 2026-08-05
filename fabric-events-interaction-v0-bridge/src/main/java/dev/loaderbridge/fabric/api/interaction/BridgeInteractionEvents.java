package dev.loaderbridge.fabric.api.interaction;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class BridgeInteractionEvents {
    private static final ThreadLocal<PendingBreak> PENDING_BREAK = new ThreadLocal<>();

    private BridgeInteractionEvents() {
    }

    public static void clearPendingBreak() {
        PENDING_BREAK.remove();
    }

    public static void setPendingBreak(LevelAccessor world, Player player, BlockPos pos,
            BlockState state, @Nullable BlockEntity entity) {
        PENDING_BREAK.set(new PendingBreak(world, player, pos.immutable(), state, entity));
    }

    public static void finishPendingBreak(BlockPos pos, boolean broken) {
        PendingBreak pending = PENDING_BREAK.get();
        PENDING_BREAK.remove();
        if (!broken || pending == null || !pending.pos().equals(pos)) return;
        if (!(pending.world() instanceof net.minecraft.world.level.Level level)) return;
        PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(
                level, pending.player(), pending.pos(), pending.state(), pending.entity());
    }

    private record PendingBreak(LevelAccessor world, Player player, BlockPos pos,
            BlockState state, @Nullable BlockEntity entity) {
    }
}
