package dev.loaderbridge.fabric.api.item;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.item.v1.EnchantmentEvents;
import net.fabricmc.fabric.api.item.v1.EnchantmentSource;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.fabric.api.item.v1.FabricItem;
import net.minecraftforge.fml.common.Mod;
import org.junit.jupiter.api.Test;

class FabricItemApiContractTest {
    @Test
    void providerPinsOnlyTheImplementedCommonSurface() throws IOException {
        var descriptor = new FabricItemApiBridgeProvider().descriptor();
        assertThat(descriptor.contractVersion()).isEqualTo("fabric-item-api-v1:11.3.0");
        assertThat(descriptor.implementationVersion())
                .isEqualTo("11.3.0+467044f319-loaderbridge.4");
        assertThat(descriptor.providedClasses()).containsExactlyInAnyOrder(
                "net.fabricmc.fabric.api.item.v1.CustomDamageHandler",
                "net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents",
                "net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents$ModifyCallback",
                "net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents$ModifyContext",
                "net.fabricmc.fabric.api.item.v1.EnchantingContext",
                "net.fabricmc.fabric.api.item.v1.EnchantmentEvents",
                "net.fabricmc.fabric.api.item.v1.EnchantmentEvents$AllowEnchanting",
                "net.fabricmc.fabric.api.item.v1.EnchantmentEvents$Modify",
                "net.fabricmc.fabric.api.item.v1.EnchantmentSource",
                "net.fabricmc.fabric.api.item.v1.EquipmentSlotProvider",
                "net.fabricmc.fabric.api.item.v1.FabricComponentMapBuilder",
                "net.fabricmc.fabric.api.item.v1.FabricItem",
                "net.fabricmc.fabric.api.item.v1.FabricItem$Settings",
                "net.fabricmc.fabric.api.item.v1.FabricItemStack",
                "net.fabricmc.fabric.api.item.v1.FabricTooltipType");
        assertThat(descriptor.requiredModules()).containsExactly("fabric-api-base-bridge");
        assertThat(FabricItemApiBridgeMod.class.getAnnotation(Mod.class).value())
                .isEqualTo("loaderbridge_fabric_item_api_v1");
        try (var metadata = getClass().getResourceAsStream("/META-INF/mods.toml")) {
            assertThat(metadata).isNotNull();
            assertThat(new String(metadata.readAllBytes(), StandardCharsets.UTF_8))
                    .contains("version=\"11.3.0.4\"");
        }
    }

    @Test
    void enchantingEventsPreserveFabricOrderingAndShortCircuiting() {
        AtomicInteger calls = new AtomicInteger();
        EnchantmentEvents.ALLOW_ENCHANTING.register((enchantment, stack, context) -> {
            calls.incrementAndGet();
            return TriState.DEFAULT;
        });
        EnchantmentEvents.ALLOW_ENCHANTING.register((enchantment, stack, context) -> {
            calls.incrementAndGet();
            return TriState.TRUE;
        });
        EnchantmentEvents.ALLOW_ENCHANTING.register((enchantment, stack, context) -> {
            calls.incrementAndGet();
            return TriState.FALSE;
        });

        assertThat(EnchantmentEvents.ALLOW_ENCHANTING.invoker()
                .allowEnchanting(null, null, null)).isEqualTo(TriState.TRUE);
        assertThat(calls).hasValue(2);
    }

    @Test
    void defaultMethodsRetainFabricFallbacks() {
        FabricItem fabricItem = new FabricItem() { };
        assertThat(fabricItem.allowComponentsUpdateAnimation(null, null, null, null)).isTrue();
        assertThat(fabricItem.allowContinuingBlockBreaking(null, null, null)).isFalse();
        assertThat(EnchantmentSource.VANILLA.isBuiltin()).isTrue();
        assertThat(EnchantmentSource.MOD.isBuiltin()).isTrue();
        assertThat(EnchantmentSource.DATA_PACK.isBuiltin()).isFalse();
    }
}
