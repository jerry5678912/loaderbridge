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

public final class AllIngredient extends CombinedIngredient {
    private static final MapCodec<AllIngredient> ALLOW_EMPTY_CODEC = createCodec(Ingredient.CODEC);
    private static final MapCodec<AllIngredient> DISALLOW_EMPTY_CODEC = createCodec(Ingredient.CODEC_NONEMPTY);
    public static final CustomIngredientSerializer<AllIngredient> SERIALIZER = new Serializer<>(
            ResourceLocation.fromNamespaceAndPath("fabric", "all"), AllIngredient::new,
            ALLOW_EMPTY_CODEC, DISALLOW_EMPTY_CODEC);

    public AllIngredient(List<Ingredient> ingredients) { super(ingredients); }

    private static MapCodec<AllIngredient> createCodec(Codec<Ingredient> codec) {
        return codec.listOf().fieldOf("ingredients").xmap(AllIngredient::new, AllIngredient::getIngredients);
    }

    @Override public boolean test(ItemStack stack) {
        return ingredients.stream().allMatch(ingredient -> ingredient.test(stack));
    }

    @Override public List<ItemStack> getMatchingStacks() {
        List<ItemStack> preview = new ArrayList<>(Arrays.asList(ingredients.getFirst().getItems()));
        for (int index = 1; index < ingredients.size(); index++) {
            Ingredient ingredient = ingredients.get(index);
            preview.removeIf(stack -> !ingredient.test(stack));
        }
        return preview;
    }

    @Override public CustomIngredientSerializer<?> getSerializer() { return SERIALIZER; }
}
