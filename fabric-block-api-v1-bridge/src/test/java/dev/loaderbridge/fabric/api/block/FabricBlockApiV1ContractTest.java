package dev.loaderbridge.fabric.api.block;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import net.fabricmc.fabric.api.block.v1.BlockFunctionalityTags;
import net.fabricmc.fabric.api.block.v1.FabricBlock;
import net.fabricmc.fabric.api.block.v1.FabricBlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.common.Mod;
import org.junit.jupiter.api.Test;

class FabricBlockApiV1ContractTest {
    @Test
    void providerPinsEveryPublicType() {
        var descriptor = new FabricBlockApiV1BridgeProvider().descriptor();
        assertThat(descriptor.contractVersion()).isEqualTo("fabric-block-api-v1:1.1.0");
        assertThat(descriptor.implementationVersion())
                .isEqualTo("1.1.0+0bc3503219-loaderbridge.1");
        assertThat(descriptor.providedClasses()).containsExactlyInAnyOrderElementsOf(Set.of(
                BlockFunctionalityTags.class.getName(),
                FabricBlock.class.getName(),
                FabricBlockState.class.getName()));
        assertThat(descriptor.providedModVersions())
                .containsEntry("fabric-block-api-v1", "1.1.0+0bc3503219");
    }

    @Test
    void forgeHostContainerMatchesPackagedModIdentity() {
        assertThat(FabricBlockApiV1BridgeMod.class.getAnnotation(Mod.class).value())
                .isEqualTo("loaderbridge_fabric_block_api_v1");
    }

    @Test
    void defaultBlockAppearanceIsItsOriginalState() {
        FabricBlock block = new FabricBlock() { };
        assertThat(block.getAppearance(null, EmptyBlockGetter.INSTANCE, BlockPos.ZERO,
                Direction.NORTH, null, null)).isNull();
    }

    @Test
    void blockAndStateContractsUseFabricExactBlockGetterSignatures() throws Exception {
        assertThat(FabricBlock.class.getDeclaredMethod("getAppearance",
                BlockState.class, net.minecraft.world.level.BlockGetter.class,
                BlockPos.class, Direction.class, BlockState.class, BlockPos.class)
                .getReturnType()).isEqualTo(BlockState.class);
        assertThat(FabricBlockState.class.getDeclaredMethod("getAppearance",
                net.minecraft.world.level.BlockGetter.class, BlockPos.class,
                Direction.class, BlockState.class, BlockPos.class)
                .getReturnType()).isEqualTo(BlockState.class);
    }

    @Test
    void functionalityTagUsesFabricNamespaceAndPinnedPath() {
        assertThat(BlockFunctionalityTags.CAN_CLIMB_TRAPDOOR_ABOVE.location().toString())
                .isEqualTo("fabric:can_climb_trapdoor_above");
    }
}
