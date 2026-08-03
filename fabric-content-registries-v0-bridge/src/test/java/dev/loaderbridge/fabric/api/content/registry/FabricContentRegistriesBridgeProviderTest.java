package dev.loaderbridge.fabric.api.content.registry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class FabricContentRegistriesBridgeProviderTest {
    @Test
    void advertisesOnlyImplementedContentRegistryClasses() {
        var descriptor = new FabricContentRegistriesBridgeProvider().descriptor();
        assertThat(descriptor.implementationVersion())
                .isEqualTo("8.0.19+b559734419-loaderbridge.1");
        assertThat(descriptor.providedModVersions())
                .containsEntry("fabric-content-registries-v0", "8.0.19+b559734419");
        assertThat(descriptor.providedClasses()).containsExactlyInAnyOrderElementsOf(Set.of(
                "net.fabricmc.fabric.api.util.Item2ObjectMap",
                "net.fabricmc.fabric.api.util.Block2ObjectMap",
                "net.fabricmc.fabric.api.registry.FuelRegistry",
                "net.fabricmc.fabric.api.registry.CompostingChanceRegistry",
                "net.fabricmc.fabric.api.registry.FlammableBlockRegistry",
                "net.fabricmc.fabric.api.registry.FlammableBlockRegistry$Entry",
                "net.fabricmc.fabric.api.registry.FlattenableBlockRegistry",
                "net.fabricmc.fabric.api.registry.StrippableBlockRegistry",
                "net.fabricmc.fabric.api.registry.OxidizableBlocksRegistry"));
    }
}
