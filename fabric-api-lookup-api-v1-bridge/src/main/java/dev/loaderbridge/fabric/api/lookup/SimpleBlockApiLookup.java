package dev.loaderbridge.fabric.api.lookup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Lookup implementation preserving Fabric's provider-first, fallback-second ordering. */
final class SimpleBlockApiLookup<A, C> implements BlockApiLookup<A, C> {
    private final ResourceLocation id;
    private final Class<A> apiClass;
    private final Class<C> contextClass;
    private final Map<Block, BlockApiProvider<A, C>> providers = new ConcurrentHashMap<>();
    private final List<BlockApiProvider<A, C>> fallbacks = new CopyOnWriteArrayList<>();

    SimpleBlockApiLookup(ResourceLocation id, Class<A> apiClass, Class<C> contextClass) {
        this.id = id;
        this.apiClass = apiClass;
        this.contextClass = contextClass;
    }

    @Override
    public A find(Level world, BlockPos pos, BlockState state, BlockEntity blockEntity,
            C context) {
        Objects.requireNonNull(world, "World may not be null.");
        Objects.requireNonNull(pos, "BlockPos may not be null.");
        if (blockEntity == null) {
            if (state == null) state = world.getBlockState(pos);
            if (state.hasBlockEntity()) blockEntity = world.getBlockEntity(pos);
        } else if (state == null) {
            state = blockEntity.getBlockState();
        }
        BlockApiProvider<A, C> provider = providers.get(state.getBlock());
        if (provider != null) {
            A result = provider.find(world, pos, state, blockEntity, context);
            if (result != null) return result;
        }
        for (BlockApiProvider<A, C> fallback : fallbacks) {
            A result = fallback.find(world, pos, state, blockEntity, context);
            if (result != null) return result;
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerSelf(BlockEntityType<?>... blockEntityTypes) {
        for (BlockEntityType<?> type : checkedTypes(blockEntityTypes)) {
            Block support = supportedBlocks(type).getFirst();
            BlockEntity blockEntity = Objects.requireNonNull(
                    type.create(BlockPos.ZERO, support.defaultBlockState()),
                    "Instantiated block entity may not be null.");
            if (!apiClass.isAssignableFrom(blockEntity.getClass())) {
                throw new IllegalArgumentException("API class " + apiClass.getCanonicalName()
                        + " is not assignable from block entity class "
                        + blockEntity.getClass().getCanonicalName());
            }
        }
        registerForBlockEntities((blockEntity, context) -> (A) blockEntity, blockEntityTypes);
    }

    @Override
    public void registerForBlocks(BlockApiProvider<A, C> provider, Block... blocks) {
        Objects.requireNonNull(provider, "BlockApiProvider may not be null.");
        if (blocks.length == 0) {
            throw new IllegalArgumentException("Must register at least one Block instance");
        }
        for (Block block : blocks) {
            providers.putIfAbsent(Objects.requireNonNull(block,
                    "Encountered null block while registering a provider"), provider);
        }
    }

    @Override
    public void registerForBlockEntities(BlockEntityApiProvider<A, C> provider,
            BlockEntityType<?>... blockEntityTypes) {
        Objects.requireNonNull(provider, "BlockEntityApiProvider may not be null.");
        for (BlockEntityType<?> type : checkedTypes(blockEntityTypes)) {
            BlockApiProvider<A, C> wrapper = (world, pos, state, blockEntity, context) ->
                    blockEntity == null || blockEntity.getType() != type
                            ? null : provider.find(blockEntity, context);
            registerForBlocks(wrapper, supportedBlocks(type).toArray(Block[]::new));
        }
    }

    private static List<BlockEntityType<?>> checkedTypes(BlockEntityType<?>[] types) {
        if (types.length == 0) {
            throw new IllegalArgumentException("Must register at least one BlockEntityType instance");
        }
        List<BlockEntityType<?>> checked = new ArrayList<>(types.length);
        for (BlockEntityType<?> type : types) checked.add(Objects.requireNonNull(type));
        return checked;
    }

    private static List<Block> supportedBlocks(BlockEntityType<?> type) {
        List<Block> blocks = BuiltInRegistries.BLOCK.stream()
                .filter(block -> type.isValid(block.defaultBlockState())).toList();
        if (blocks.isEmpty()) {
            throw new IllegalArgumentException("Block entity type has no supported blocks");
        }
        return blocks;
    }

    @Override public void registerFallback(BlockApiProvider<A, C> provider) {
        fallbacks.add(Objects.requireNonNull(provider, "BlockApiProvider may not be null."));
    }
    @Override public ResourceLocation getId() { return id; }
    @Override public Class<A> apiClass() { return apiClass; }
    @Override public Class<C> contextClass() { return contextClass; }
    @Override public BlockApiProvider<A, C> getProvider(Block block) { return providers.get(block); }
}
