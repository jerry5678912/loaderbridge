package net.fabricmc.fabric.api.object.builder.v1.block.entity;

import com.mojang.datafixers.types.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Binary-compatible implementation of Fabric's 1.21.1 block entity type builder. */
@Deprecated
public final class FabricBlockEntityTypeBuilder<T extends BlockEntity> {
    private final Factory<? extends T> factory;
    private final List<Block> blocks;

    private FabricBlockEntityTypeBuilder(Factory<? extends T> factory, List<Block> blocks) {
        this.factory = factory;
        this.blocks = blocks;
    }

    @Deprecated
    public static <T extends BlockEntity> FabricBlockEntityTypeBuilder<T> create(
            Factory<? extends T> factory, Block... blocks) {
        List<Block> supportedBlocks = new ArrayList<>(blocks.length);
        Collections.addAll(supportedBlocks, blocks);
        return new FabricBlockEntityTypeBuilder<>(factory, supportedBlocks);
    }

    @Deprecated
    public FabricBlockEntityTypeBuilder<T> addBlock(Block block) {
        blocks.add(block);
        return this;
    }

    @Deprecated
    public FabricBlockEntityTypeBuilder<T> addBlocks(Block... blocks) {
        Collections.addAll(this.blocks, blocks);
        return this;
    }

    @Deprecated
    public BlockEntityType<T> build() {
        return build(null);
    }

    @Deprecated
    public BlockEntityType<T> build(Type<?> type) {
        return BlockEntityType.Builder.<T>of(factory::create, blocks.toArray(Block[]::new)).build(type);
    }

    @FunctionalInterface
    @Deprecated
    public interface Factory<T extends BlockEntity> {
        T create(BlockPos blockPos, BlockState blockState);
    }
}
