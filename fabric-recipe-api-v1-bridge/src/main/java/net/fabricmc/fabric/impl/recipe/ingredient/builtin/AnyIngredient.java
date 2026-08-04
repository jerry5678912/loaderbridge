package net.fabricmc.fabric.impl.recipe.ingredient.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public final class AnyIngredient extends CombinedIngredient {
    private static final MapCodec<AnyIngredient> ALLOW_EMPTY_CODEC = createCodec(Ingredient.CODEC);
    private static final MapCodec<AnyIngredient> DISALLOW_EMPTY_CODEC = createCodec(Ingredient.CODEC_NONEMPTY);
    public static final CustomIngredientSerializer<AnyIngredient> SERIALIZER = new Serializer<>(
            ResourceLocation.fromNamespaceAndPath("fabric", "any"), AnyIngredient::new,
            ALLOW_EMPTY_CODEC, DISALLOW_EMPTY_CODEC);

    public AnyIngredient(List<Ingredient> ingredients) { super(ingredients); }

    private static MapCodec<AnyIngredient> createCodec(Codec<Ingredient> codec) {
        return codec.listOf().fieldOf("ingredients").xmap(AnyIngredient::new, AnyIngredient::getIngredients);
    }

    @Override public boolean test(ItemStack stack) {
        return ingredients.stream().anyMatch(ingredient -> ingredient.test(stack));
    }

    @Override public List<ItemStack> getMatchingStacks() {
        List<ItemStack> preview = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            preview.addAll(Arrays.asList(ingredient.getItems()));
        }
        return preview;
    }

    @Override public CustomIngredientSerializer<?> getSerializer() { return SERIALIZER; }
}
