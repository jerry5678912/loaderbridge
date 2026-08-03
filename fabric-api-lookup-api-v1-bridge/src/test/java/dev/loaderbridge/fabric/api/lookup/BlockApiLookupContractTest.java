package dev.loaderbridge.fabric.api.lookup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class BlockApiLookupContractTest {
    @Test
    void providerPinsOnlyTheImplementedBlockLookupSurface() {
        var descriptor = new FabricApiLookupBridgeProvider().descriptor();

        assertThat(descriptor.contractVersion()).isEqualTo("fabric-api-lookup-api-v1:1.6.72");
        assertThat(descriptor.implementationVersion()).isEqualTo("1.6.72+d30f6a7919-loaderbridge.1");
        assertThat(descriptor.providedClasses()).containsExactlyInAnyOrder(
                "net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup",
                "net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup$BlockApiProvider",
                "net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup$BlockEntityApiProvider",
                "net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache");
    }

    @Test
    void lookupIdentityAndTypeSafetyMatchThePublicContract() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                "loaderbridge_test", "string_lookup");
        BlockApiLookup<String, Void> first = BlockApiLookup.get(id, String.class, Void.class);
        BlockApiLookup<String, Void> second = BlockApiLookup.get(id, String.class, Void.class);

        assertThat(second).isSameAs(first);
        assertThat(first.getId()).isEqualTo(id);
        assertThat(first.apiClass()).isEqualTo(String.class);
        assertThat(first.contextClass()).isEqualTo(Void.class);
        assertThatThrownBy(() -> BlockApiLookup.get(id, Integer.class, Void.class))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blockProviderRegistrationRejectsAnEmptyTargetSet() {
        var lookup = BlockApiLookup.get(ResourceLocation.fromNamespaceAndPath(
                "loaderbridge_test", "block_provider"), String.class, Void.class);
        assertThatThrownBy(() -> lookup.registerForBlocks(
                (world, pos, state, blockEntity, context) -> "value"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exposesThePinnedCacheContract() throws ReflectiveOperationException {
        assertThat(BlockApiCache.class.getDeclaredMethod("find", Object.class).getReturnType())
                .isEqualTo(Object.class);
        assertThat(BlockApiCache.class.getDeclaredMethod("getLookup").getReturnType())
                .isEqualTo(BlockApiLookup.class);
    }
}
