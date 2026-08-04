package net.fabricmc.fabric.impl.recipe.ingredient;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.FabricIngredient;
import net.minecraft.world.item.crafting.Ingredient;

public final class FabricIngredientCodecs {
    public static MapCodec<CustomIngredient> customCodec(boolean allowEmpty) {
        return CustomIngredientImpl.CODEC.dispatchMap(
                CustomIngredientImpl.TYPE_KEY,
                CustomIngredient::getSerializer,
                serializer -> serializer.getCodec(allowEmpty));
    }

    public static Codec<Ingredient> enhance(Codec<Ingredient> original, boolean allowEmpty) {
        return Codec.either(customCodec(allowEmpty).codec(), original).xmap(
                either -> either.map(CustomIngredient::toVanilla, ingredient -> ingredient),
                ingredient -> {
                    CustomIngredient custom = ((FabricIngredient) ingredient).getCustomIngredient();
                    return custom == null ? Either.right(ingredient) : Either.left(custom);
                });
    }

    private FabricIngredientCodecs() { }
}
