package dev.loaderbridge.fabric.api.biome;

import static org.assertj.core.api.Assertions.assertThat;

import com.mojang.serialization.Lifecycle;
import java.util.Set;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.api.biome.v1.NetherBiomes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.junit.jupiter.api.Test;

class FabricBiomeContractTest {
    @Test
    void advertisesOnlyImplementedBiomeContracts() {
        var descriptor = new FabricBiomeBridgeProvider().descriptor();
        assertThat(descriptor.implementationVersion()).isEqualTo("13.0.31+d527f9fd19-loaderbridge.6");
        assertThat(descriptor.providedClasses()).containsExactlyInAnyOrderElementsOf(Set.of(
                "net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext",
                "net.fabricmc.fabric.api.biome.v1.BiomeSelectors",
                "net.fabricmc.fabric.api.biome.v1.BiomeModifications",
                "net.fabricmc.fabric.api.biome.v1.BiomeModification",
                "net.fabricmc.fabric.api.biome.v1.BiomeModificationContext",
                "net.fabricmc.fabric.api.biome.v1.BiomeModificationContext$WeatherContext",
                "net.fabricmc.fabric.api.biome.v1.BiomeModificationContext$EffectsContext",
                "net.fabricmc.fabric.api.biome.v1.BiomeModificationContext$GenerationSettingsContext",
                "net.fabricmc.fabric.api.biome.v1.BiomeModificationContext$SpawnSettingsContext",
                "net.fabricmc.fabric.api.biome.v1.ModificationPhase",
                "net.fabricmc.fabric.api.biome.v1.NetherBiomes"));
    }

    @Test
    void addFeatureRecordsGenericRuleForForgeDatapackResolution() {
        int before = BridgeBiomeRules.genericRuleCount();
        ResourceKey<PlacedFeature> key = ResourceKey.create(Registries.PLACED_FEATURE,
                ResourceLocation.fromNamespaceAndPath("fixture", "ore"));
        BiomeModifications.addFeature(BiomeSelectors.all(),
                GenerationStep.Decoration.UNDERGROUND_ORES, key);
        assertThat(BridgeBiomeRules.genericRuleCount()).isEqualTo(before + 1);
    }

    @Test
    void addCarverRecordsRuleForDatapackResolution() {
        int carversBefore = BridgeBiomeRules.genericRuleCount();
        BiomeModifications.addCarver(BiomeSelectors.all(),
                GenerationStep.Carving.AIR, Carvers.NETHER_CAVE);
        assertThat(BridgeBiomeRules.genericRuleCount()).isEqualTo(carversBefore + 1);
    }

    @Test
    void genericModificationRegistersContextFreeAndContextSensitiveRules() {
        int before = BridgeBiomeRules.genericRuleCount();
        var modification = BiomeModifications.create(
                ResourceLocation.fromNamespaceAndPath("fixture", "ordered"));
        assertThat(modification.add(ModificationPhase.REMOVALS, BiomeSelectors.all(),
                context -> context.getSpawnSettings().clearSpawns())).isSameAs(modification);
        assertThat(modification.add(ModificationPhase.POST_PROCESSING, BiomeSelectors.all(),
                (selection, context) -> context.getWeather().setTemperature(0.42F)))
                .isSameAs(modification);
        assertThat(BridgeBiomeRules.genericRuleCount()).isEqualTo(before + 2);
    }

    @Test
    void reverseRegistryLookupUsesRegisteredObjectIdentity() {
        ResourceKey<Registry<String>> registryKey = ResourceKey.createRegistryKey(
                ResourceLocation.fromNamespaceAndPath("fixture", "values"));
        MappedRegistry<String> registry = new MappedRegistry<>(registryKey, Lifecycle.stable());
        ResourceKey<String> valueKey = ResourceKey.create(registryKey,
                ResourceLocation.fromNamespaceAndPath("fixture", "registered"));
        String registered = new String("value");
        registry.register(valueKey, registered, RegistrationInfo.BUILT_IN);

        var keys = BridgeBiomeRules.registryKeys(registry.asLookup());
        assertThat(keys.get(registered)).isEqualTo(valueKey);
        assertThat(keys.get(new String("value"))).isNull();
    }

    @Test
    void netherBiomeRegistrationAcceptsFabricParameterPoints() {
        NetherBiomes.addNetherBiome(Biomes.PLAINS,
                Climate.parameters(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        assertThat(NetherBiomes.canGenerateInNether(Biomes.PLAINS)).isTrue();
    }
}
