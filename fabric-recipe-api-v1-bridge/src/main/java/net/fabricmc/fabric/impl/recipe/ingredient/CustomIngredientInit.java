package net.fabricmc.fabric.impl.recipe.ingredient;

import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.fabricmc.fabric.impl.recipe.ingredient.builtin.AllIngredient;
import net.fabricmc.fabric.impl.recipe.ingredient.builtin.AnyIngredient;
import net.fabricmc.fabric.impl.recipe.ingredient.builtin.ComponentsIngredient;
import net.fabricmc.fabric.impl.recipe.ingredient.builtin.CustomDataIngredient;
import net.fabricmc.fabric.impl.recipe.ingredient.builtin.DifferenceIngredient;

public final class CustomIngredientInit {
    private static boolean initialized;

    public static synchronized void initialize() {
        if (initialized) return;
        CustomIngredientSerializer.register(AllIngredient.SERIALIZER);
        CustomIngredientSerializer.register(AnyIngredient.SERIALIZER);
        CustomIngredientSerializer.register(DifferenceIngredient.SERIALIZER);
        CustomIngredientSerializer.register(ComponentsIngredient.SERIALIZER);
        CustomIngredientSerializer.register(CustomDataIngredient.SERIALIZER);
        initialized = true;
    }

    private CustomIngredientInit() { }
}
