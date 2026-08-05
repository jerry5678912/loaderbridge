package dev.loaderbridge.fabric.api.block.mixin;

import net.fabricmc.fabric.api.block.v1.FabricBlockState;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockState.class)
public abstract class BlockStateMixin implements FabricBlockState {
}
