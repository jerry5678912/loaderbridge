package dev.loaderbridge.fabric.api.object.builder.mixin;

import com.mojang.datafixers.types.Type;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlockEntityType.Builder.class)
public abstract class BlockEntityTypeBuilderFabricMixin<T extends BlockEntity>
        implements FabricBlockEntityType.Builder<T> {
    @Shadow public abstract BlockEntityType<T> build(Type<?> dataType);

    @Override public BlockEntityType<T> build() {
        return build(null);
    }
}
