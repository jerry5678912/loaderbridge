package dev.loaderbridge.fabric.api.object.builder.mixin;

import com.mojang.datafixers.types.Type;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityType.class)
public abstract class BlockEntityTypeFabricMixin<T extends BlockEntity>
        implements FabricBlockEntityType {
    @Mutable @Shadow @Final private Set<Block> validBlocks;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void loaderbridge$makeSupportedBlocksMutable(
            BlockEntityType.BlockEntitySupplier<? extends T> factory,
            Set<Block> blocks, Type<?> dataType, CallbackInfo callback) {
        validBlocks = new HashSet<>(validBlocks);
    }

    @Override public void addSupportedBlock(Block block) {
        validBlocks.add(Objects.requireNonNull(block, "block"));
    }
}
