package net.fabricmc.fabric.api.recipe.v1.ingredient;

import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import net.fabricmc.fabric.impl.recipe.ingredient.builtin.AllIngredient;
import net.fabricmc.fabric.impl.recipe.ingredient.builtin.AnyIngredient;
import net.fabricmc.fabric.impl.recipe.ingredient.builtin.ComponentsIngredient;
import net.fabricmc.fabric.impl.recipe.ingredient.builtin.CustomDataIngredient;
import net.fabricmc.fabric.impl.recipe.ingredient.builtin.DifferenceIngredient;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** Factory methods for Fabric's built-in custom ingredients. */
public final class DefaultCustomIngredients {
    public static Ingredient all(Ingredient... ingredients) {
        requireElements(ingredients);
        return new AllIngredient(List.of(ingredients)).toVanilla();
    }

    public static Ingredient any(Ingredient... ingredients) {
        requireElements(ingredients);
        return new AnyIngredient(List.of(ingredients)).toVanilla();
    }

    public static Ingredient difference(Ingredient base, Ingredient subtracted) {
        return new DifferenceIngredient(
                Objects.requireNonNull(base, "Base ingredient cannot be null"),
                Objects.requireNonNull(subtracted, "Subtracted ingredient cannot be null"))
                .toVanilla();
    }

    public static Ingredient components(Ingredient base, DataComponentPatch components) {
        return new ComponentsIngredient(
                Objects.requireNonNull(base, "Base ingredient cannot be null"),
                Objects.requireNonNull(components, "Component changes cannot be null"))
                .toVanilla();
    }

    public static Ingredient components(Ingredient base,
            UnaryOperator<DataComponentPatch.Builder> operator) {
        Objects.requireNonNull(operator, "Component operator cannot be null");
        return components(base, operator.apply(DataComponentPatch.builder()).build());
    }

    public static Ingredient components(ItemStack stack) {
        Objects.requireNonNull(stack, "Stack cannot be null");
        return components(Ingredient.of(stack.getItem()), stack.getComponentsPatch());
    }

    public static Ingredient customData(Ingredient base, CompoundTag nbt) {
        return new CustomDataIngredient(base, nbt).toVanilla();
    }

    private static void requireElements(Ingredient[] ingredients) {
        Objects.requireNonNull(ingredients, "Ingredients cannot be null");
        if (ingredients.length == 0) {
            throw new IllegalArgumentException("ALL or ANY ingredient must have at least one sub-ingredient");
        }
        for (Ingredient ingredient : ingredients) {
            Objects.requireNonNull(ingredient, "Ingredient cannot be null");
        }
    }

    private DefaultCustomIngredients() { }
}
