package dev.loaderbridge.fabric.api.biome;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.junit.jupiter.api.Test;

class FabricBiomeContractTest {
    @Test
    void advertisesOnlyImplementedBiomeContracts() {
        var descriptor = new FabricBiomeBridgeProvider().descriptor();
        assertThat(descriptor.implementationVersion()).isEqualTo("13.0.31+d527f9fd19-loaderbridge.1");
        assertThat(descriptor.providedClasses()).containsExactlyInAnyOrderElementsOf(Set.of(
                "net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext",
                "net.fabricmc.fabric.api.biome.v1.BiomeSelectors",
                "net.fabricmc.fabric.api.biome.v1.BiomeModifications"));
    }

    @Test
    void addFeatureRecordsGenericRuleForForgeDatapackResolution() {
        int before = BridgeBiomeRules.featureRuleCount();
        ResourceKey<PlacedFeature> key = ResourceKey.create(Registries.PLACED_FEATURE,
                ResourceLocation.fromNamespaceAndPath("fixture", "ore"));
        BiomeModifications.addFeature(BiomeSelectors.all(),
                GenerationStep.Decoration.UNDERGROUND_ORES, key);
        assertThat(BridgeBiomeRules.featureRuleCount()).isEqualTo(before + 1);
    }
}
