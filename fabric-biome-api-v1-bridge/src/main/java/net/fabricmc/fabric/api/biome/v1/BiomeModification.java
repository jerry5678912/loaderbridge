package net.fabricmc.fabric.api.biome.v1;

import dev.loaderbridge.fabric.api.biome.BridgeBiomeRules;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;

public class BiomeModification {
    private final ResourceLocation id;

    BiomeModification(ResourceLocation id) {
        this.id = id;
    }

    public BiomeModification add(ModificationPhase phase,
            Predicate<BiomeSelectionContext> selector,
            Consumer<BiomeModificationContext> modifier) {
        BridgeBiomeRules.addModification(id, phase, selector,
                (selection, context) -> modifier.accept(context));
        return this;
    }

    public BiomeModification add(ModificationPhase phase,
            Predicate<BiomeSelectionContext> selector,
            BiConsumer<BiomeSelectionContext, BiomeModificationContext> modifier) {
        BridgeBiomeRules.addModification(id, phase, selector, modifier);
        return this;
    }
}
