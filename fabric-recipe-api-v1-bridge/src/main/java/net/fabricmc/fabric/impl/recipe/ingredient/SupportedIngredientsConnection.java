package net.fabricmc.fabric.impl.recipe.ingredient;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public interface SupportedIngredientsConnection {
    void loaderbridge$setSupportedCustomIngredients(Set<ResourceLocation> serializers);

    Set<ResourceLocation> loaderbridge$getSupportedCustomIngredients();
}
