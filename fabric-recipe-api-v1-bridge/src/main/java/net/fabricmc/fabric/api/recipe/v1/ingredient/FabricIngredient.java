package net.fabricmc.fabric.api.recipe.v1.ingredient;

import org.jetbrains.annotations.Nullable;

/** Methods injected onto every Minecraft ingredient. */
public interface FabricIngredient {
    @Nullable
    default CustomIngredient getCustomIngredient() {
        return null;
    }

    default boolean requiresTesting() {
        CustomIngredient custom = getCustomIngredient();
        return custom != null && custom.requiresTesting();
    }
}
