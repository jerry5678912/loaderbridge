package net.fabricmc.fabric.api.lookup.v1.block;

import dev.loaderbridge.fabric.api.lookup.BlockApiLookupRegistry;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Binary-compatible Fabric block API lookup contract. */
public interface BlockApiLookup<A, C> {
    static <A, C> BlockApiLookup<A, C> get(ResourceLocation id, Class<A> apiClass,
            Class<C> contextClass) {
        return BlockApiLookupRegistry.get(id, apiClass, contextClass);
    }

    default A find(Level world, BlockPos pos, C context) {
        return find(world, pos, null, null, context);
    }

    A find(Level world, BlockPos pos, BlockState state, BlockEntity blockEntity, C context);

    void registerSelf(BlockEntityType<?>... blockEntityTypes);

    void registerForBlocks(BlockApiProvider<A, C> provider, Block... blocks);

    @SuppressWarnings("unchecked")
    default <T extends BlockEntity> void registerForBlockEntity(
            BiFunction<? super T, C, A> provider, BlockEntityType<T> blockEntityType) {
        registerForBlockEntities((blockEntity, context) -> provider.apply((T) blockEntity, context),
                blockEntityType);
    }

    void registerForBlockEntities(BlockEntityApiProvider<A, C> provider,
            BlockEntityType<?>... blockEntityTypes);

    void registerFallback(BlockApiProvider<A, C> fallbackProvider);

    ResourceLocation getId();

    Class<A> apiClass();

    Class<C> contextClass();

    BlockApiProvider<A, C> getProvider(Block block);

    @FunctionalInterface
    interface BlockApiProvider<A, C> {
        A find(Level world, BlockPos pos, BlockState state, BlockEntity blockEntity, C context);
    }

    @FunctionalInterface
    interface BlockEntityApiProvider<A, C> {
        A find(BlockEntity blockEntity, C context);
    }
}
