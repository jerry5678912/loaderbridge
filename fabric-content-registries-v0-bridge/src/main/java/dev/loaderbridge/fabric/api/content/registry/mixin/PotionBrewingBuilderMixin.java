package dev.loaderbridge.fabric.api.content.registry.mixin;

import java.util.List;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PotionBrewing.Builder.class)
public abstract class PotionBrewingBuilderMixin
        implements FabricBrewingRecipeRegistryBuilder {
    @Shadow @Final private FeatureFlagSet enabledFeatures;
    @Shadow @Final private List<PotionBrewing.Mix<Item>> containerMixes;
    @Shadow @Final private List<PotionBrewing.Mix<Potion>> potionMixes;
    @Shadow private static void expectPotion(Item item) { }

    @Override
    @SuppressWarnings("deprecation")
    public void registerItemRecipe(Item input, Ingredient ingredient, Item output) {
        if (!input.isEnabled(enabledFeatures) || !output.isEnabled(enabledFeatures)) return;
        expectPotion(input);
        expectPotion(output);
        containerMixes.add(new PotionBrewing.Mix<>(input.builtInRegistryHolder(),
                ingredient, output.builtInRegistryHolder()));
    }

    @Override
    public void registerPotionRecipe(
            Holder<Potion> input, Ingredient ingredient, Holder<Potion> output) {
        if (!input.value().isEnabled(enabledFeatures)
                || !output.value().isEnabled(enabledFeatures)) return;
        potionMixes.add(new PotionBrewing.Mix<>(input, ingredient, output));
    }

    @Override
    public void registerRecipes(Ingredient ingredient, Holder<Potion> potion) {
        if (!potion.value().isEnabled(enabledFeatures)) return;
        registerPotionRecipe(Potions.WATER, ingredient, Potions.MUNDANE);
        registerPotionRecipe(Potions.AWKWARD, ingredient, potion);
    }

    @Override
    public FeatureFlagSet getEnabledFeatures() {
        return enabledFeatures;
    }
}
