package dev.loaderbridge.fabric.api.block.mixin;

import net.fabricmc.fabric.api.block.v1.FabricBlock;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Block.class)
public abstract class BlockMixin implements FabricBlock {
}
