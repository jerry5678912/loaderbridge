package dev.loaderbridge.fabric.api.item;

import static org.assertj.core.api.Assertions.assertThat;

import net.fabricmc.fabric.api.item.v1.EnchantmentSource;
import net.fabricmc.fabric.api.item.v1.FabricItem;
import net.minecraftforge.fml.common.Mod;
import org.junit.jupiter.api.Test;

class FabricItemApiContractTest {
    @Test
    void providerPinsOnlyTheImplementedCommonSurface() {
        var descriptor = new FabricItemApiBridgeProvider().descriptor();
        assertThat(descriptor.contractVersion()).isEqualTo("fabric-item-api-v1:11.3.0");
        assertThat(descriptor.implementationVersion())
                .isEqualTo("11.3.0+467044f319-loaderbridge.1");
        assertThat(descriptor.providedClasses()).containsExactlyInAnyOrder(
                "net.fabricmc.fabric.api.item.v1.CustomDamageHandler",
                "net.fabricmc.fabric.api.item.v1.EquipmentSlotProvider",
                "net.fabricmc.fabric.api.item.v1.FabricComponentMapBuilder",
                "net.fabricmc.fabric.api.item.v1.FabricItem",
                "net.fabricmc.fabric.api.item.v1.FabricItem$Settings",
                "net.fabricmc.fabric.api.item.v1.FabricItemStack",
                "net.fabricmc.fabric.api.item.v1.FabricTooltipType");
        assertThat(descriptor.requiredModules()).containsExactly("fabric-api-base-bridge");
        assertThat(FabricItemApiBridgeMod.class.getAnnotation(Mod.class).value())
                .isEqualTo("loaderbridge_fabric_item_api_v1");
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
