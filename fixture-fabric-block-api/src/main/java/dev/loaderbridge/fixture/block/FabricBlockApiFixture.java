package dev.loaderbridge.fixture.block;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.block.v1.BlockFunctionalityTags;
import net.fabricmc.fabric.api.block.v1.FabricBlock;
import net.fabricmc.fabric.api.block.v1.FabricBlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

/** Runtime proof for Fabric's injected block and block-state contracts. */
public final class FabricBlockApiFixture implements ModInitializer {
    @Override
    public void onInitialize() {
        if (!(Blocks.STONE instanceof FabricBlock)
                || !(Blocks.STONE.defaultBlockState() instanceof FabricBlockState)) {
            throw new IllegalStateException(
                    "Fabric block interfaces were not injected into vanilla targets");
        }

        ResourceLocation mimicId = ResourceLocation.fromNamespaceAndPath(
                "loaderbridge_block_api_fixture", "mimic_block");
        MimicBlock mimic = Registry.register(
                BuiltInRegistries.BLOCK, mimicId, new MimicBlock());
        BlockItem mimicItem = Registry.register(BuiltInRegistries.ITEM, mimicId,
                new BlockItem(mimic, new Item.Properties()));
        BlockState apparent = ((FabricBlockState) mimic.defaultBlockState())
                .getAppearance(EmptyBlockGetter.INSTANCE, BlockPos.ZERO,
                        Direction.NORTH, Blocks.STONE.defaultBlockState(), BlockPos.ZERO);
        if (apparent != Blocks.GOLD_BLOCK.defaultBlockState()) {
            throw new IllegalStateException("Fabric block-state appearance did not delegate");
        }
        if (!BlockFunctionalityTags.CAN_CLIMB_TRAPDOOR_ABOVE.location().toString()
                .equals("fabric:can_climb_trapdoor_above")) {
            throw new IllegalStateException("Fabric functionality tag identity changed");
        }
        if (mimicItem.getBlock() != mimic
                || BuiltInRegistries.BLOCK.get(mimicId) != mimic
                || BuiltInRegistries.ITEM.get(mimicId) != mimicItem) {
            throw new IllegalStateException("controlled Fabric content was not registered");
        }
        System.out.println("LOADERBRIDGE_FABRIC_BLOCK_API_READY");
        System.out.println("LOADERBRIDGE_FABRIC_BLOCK_CONTENT_READY");
    }

    private static final class MimicBlock extends Block implements FabricBlock {
        private MimicBlock() {
            super(BlockBehaviour.Properties.of());
        }

        @Override
        public BlockState getAppearance(BlockState state, BlockGetter renderView, BlockPos pos,
                Direction side, BlockState sourceState, BlockPos sourcePos) {
            return Blocks.GOLD_BLOCK.defaultBlockState();
        }
    }
}
