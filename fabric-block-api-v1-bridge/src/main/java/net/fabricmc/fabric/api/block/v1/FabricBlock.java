package net.fabricmc.fabric.api.block.v1;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Fabric extensions injected into every Minecraft block. */
public interface FabricBlock {
    default BlockState getAppearance(
            BlockState state,
            BlockGetter renderView,
            BlockPos pos,
            Direction side,
            @Nullable BlockState sourceState,
            @Nullable BlockPos sourcePos) {
        return state;
    }
}
