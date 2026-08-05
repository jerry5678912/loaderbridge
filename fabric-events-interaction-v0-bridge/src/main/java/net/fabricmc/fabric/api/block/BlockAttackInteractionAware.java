package net.fabricmc.fabric.api.block;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** @deprecated Use {@link AttackBlockCallback} and inspect the target block. */
@Deprecated
public interface BlockAttackInteractionAware {
    boolean onAttackInteraction(BlockState state, Level world, BlockPos pos,
            Player player, InteractionHand hand, Direction direction);
}
