package net.fabricmc.fabric.api.recipe.v1.ingredient;

import java.util.List;
import net.fabricmc.fabric.impl.recipe.ingredient.CustomIngredientImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** Binary-compatible Fabric Recipe API custom ingredient contract. */
public interface CustomIngredient {
    boolean test(ItemStack stack);

    List<ItemStack> getMatchingStacks();

    boolean requiresTesting();

    CustomIngredientSerializer<?> getSerializer();

    default Ingredient toVanilla() {
        return new CustomIngredientImpl(this);
    }
}
