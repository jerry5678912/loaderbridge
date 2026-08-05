package net.fabricmc.fabric.api.transfer.v1.fluid;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Registration and level mapping for cauldrons exposed as fluid storage. */
public final class CauldronFluidContent {
    private static final Map<Block, CauldronFluidContent> BLOCK_TO_CAULDRON =
            new ConcurrentHashMap<>();
    private static final Map<Fluid, CauldronFluidContent> FLUID_TO_CAULDRON =
            new ConcurrentHashMap<>();

    static {
        registerCauldron(Blocks.CAULDRON, Fluids.EMPTY, FluidConstants.BUCKET, null);
        registerCauldron(Blocks.WATER_CAULDRON, Fluids.WATER,
                FluidConstants.BOTTLE, LayeredCauldronBlock.LEVEL);
        registerCauldron(Blocks.LAVA_CAULDRON, Fluids.LAVA,
                FluidConstants.BUCKET, null);
    }

    public final Block block;
    public final Fluid fluid;
    public final long amountPerLevel;
    public final int maxLevel;
    public final IntegerProperty levelProperty;

    private CauldronFluidContent(Block block, Fluid fluid, long amountPerLevel,
            int maxLevel, IntegerProperty levelProperty) {
        this.block = block;
        this.fluid = fluid;
        this.amountPerLevel = amountPerLevel;
        this.maxLevel = maxLevel;
        this.levelProperty = levelProperty;
    }

    public static CauldronFluidContent getForBlock(Block block) {
        return BLOCK_TO_CAULDRON.get(block);
    }

    public static CauldronFluidContent getForFluid(Fluid fluid) {
        return FLUID_TO_CAULDRON.get(fluid);
    }

    public static synchronized CauldronFluidContent registerCauldron(Block block,
            Fluid fluid, long amountPerLevel, IntegerProperty levelProperty) {
        CauldronFluidContent existing = BLOCK_TO_CAULDRON.get(block);
        if (existing != null) return existing;
        if (FLUID_TO_CAULDRON.containsKey(fluid)) {
            throw new IllegalArgumentException(
                    "Fluid already has a mapping for a different block.");
        }
        int maximum = 1;
        if (levelProperty != null) {
            Collection<Integer> levels = levelProperty.getPossibleValues();
            if (levels.isEmpty()) {
                throw new RuntimeException("Cauldron should have at least one possible level.");
            }
            int minimum = Integer.MAX_VALUE;
            maximum = 0;
            for (int level : levels) {
                minimum = Math.min(minimum, level);
                maximum = Math.max(maximum, level);
            }
            if (minimum != 1 || maximum < 1) {
                throw new IllegalStateException(
                        "Minimum level should be 1, and maximum level should be >= 1.");
            }
        }
        CauldronFluidContent content = new CauldronFluidContent(
                block, fluid, amountPerLevel, maximum, levelProperty);
        BLOCK_TO_CAULDRON.putIfAbsent(block, content);
        FLUID_TO_CAULDRON.putIfAbsent(fluid, content);
        FluidStorage.SIDED.registerForBlocks(
                (world, pos, state, blockEntity, direction) ->
                        BridgeCauldronStorage.get(world, pos), block);
        return content;
    }

    public int currentLevel(BlockState state) {
        if (fluid == Fluids.EMPTY) return 0;
        return levelProperty == null ? 1 : state.getValue(levelProperty);
    }
}
