package dev.loaderbridge.fabric.api.blockrenderlayer;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Set;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import org.junit.jupiter.api.Test;

class FabricBlockRenderLayerContractTest {
    @Test void exposesOfficialSingletonAndModule() {
        assertThat(BlockRenderLayerMap.INSTANCE).isNotNull();
        var descriptor = new FabricBlockRenderLayerBridgeProvider().descriptor();
        assertThat(descriptor.implementationVersion()).isEqualTo("1.1.52+0af3f5a719-loaderbridge.1");
        assertThat(descriptor.providedClasses()).containsExactlyInAnyOrderElementsOf(Set.of(
                "net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap"));
    }
}
