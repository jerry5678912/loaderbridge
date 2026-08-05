package net.fabricmc.fabric.api.registry;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.Holder;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.Ingredient;

public interface FabricBrewingRecipeRegistryBuilder {
    Event<BuildCallback> BUILD = EventFactory.createArrayBacked(
            BuildCallback.class, callbacks -> builder -> {
                for (BuildCallback callback : callbacks) callback.build(builder);
            });

    default void registerItemRecipe(Item input, Ingredient ingredient, Item output) {
        throw new AssertionError("Must be implemented via interface injection");
    }

    default void registerPotionRecipe(
            Holder<Potion> input, Ingredient ingredient, Holder<Potion> output) {
        throw new AssertionError("Must be implemented via interface injection");
    }

    default void registerRecipes(Ingredient ingredient, Holder<Potion> potion) {
        throw new AssertionError("Must be implemented via interface injection");
    }

    default FeatureFlagSet getEnabledFeatures() {
        throw new AssertionError("Must be implemented via interface injection");
    }

    @FunctionalInterface
    interface BuildCallback {
        void build(PotionBrewing.Builder builder);
    }
}
