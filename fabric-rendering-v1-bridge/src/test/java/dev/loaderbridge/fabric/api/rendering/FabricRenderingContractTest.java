package dev.loaderbridge.fabric.api.rendering;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Set;
import net.minecraft.client.color.item.ItemColor;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.world.level.ItemLike;
import org.junit.jupiter.api.Test;

class FabricRenderingContractTest {
    @Test void exposesColorProviderRegistriesAndModule() {
        assertThat(ColorProviderRegistry.BLOCK).isNotNull();
        assertThat(ColorProviderRegistry.ITEM).isNotNull();
        var descriptor = new FabricRenderingBridgeProvider().descriptor();
        assertThat(descriptor.implementationVersion()).isEqualTo("5.1.0+ab4c25a019-loaderbridge.3");
        assertThat(descriptor.providedClasses()).containsExactlyInAnyOrderElementsOf(Set.of(
                "net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry",
                "net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry",
                "net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry",
                "net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry",
                "net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry$TexturedModelDataProvider"));
    }

    @Test void usesAsItemIdentityRatherThanWrapperIdentity() {
        ItemColor color = (stack, tintIndex) -> 0x123456;
        // A null sentinel avoids bootstrapping Forge's global item registry in this unit test.
        ItemLike firstView = () -> null;
        ItemLike secondView = () -> null;
        ColorProviderRegistry.ITEM.register(color, firstView);

        assertThat(ColorProviderRegistry.ITEM.get(firstView)).isSameAs(color);
        assertThat(ColorProviderRegistry.ITEM.get(secondView)).isSameAs(color);
    }
}
