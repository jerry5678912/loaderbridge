package dev.loaderbridge.fabric.api.itemgroup;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class FabricItemGroupBridgeProviderTest {
    @Test
    void advertisesPinnedPublicSurfaceAndBaseDependency() {
        var descriptor = new FabricItemGroupBridgeProvider().descriptor();
        assertThat(descriptor.implementationVersion())
                .isEqualTo("4.1.7+def88e3a19-loaderbridge.1");
        assertThat(descriptor.providedClasses()).contains(
                "net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries",
                "net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents");
        assertThat(descriptor.requiredModules()).containsExactly("fabric-api-base-bridge");
    }
}
