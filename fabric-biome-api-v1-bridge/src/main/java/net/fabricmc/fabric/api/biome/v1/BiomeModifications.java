package net.fabricmc.fabric.api.biome.v1;

import dev.loaderbridge.fabric.api.biome.BridgeBiomeRules;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class BiomeModifications {
    private BiomeModifications() { }

    public static void addFeature(Predicate<BiomeSelectionContext> selector,
            GenerationStep.Decoration step, ResourceKey<PlacedFeature> featureKey) {
        BridgeBiomeRules.addFeature(selector, step, featureKey);
    }
}
