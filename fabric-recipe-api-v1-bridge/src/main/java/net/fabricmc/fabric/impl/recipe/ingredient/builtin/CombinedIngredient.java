package net.fabricmc.fabric.impl.recipe.ingredient.builtin;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.fabricmc.fabric.api.recipe.v1.ingredient.FabricIngredient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

abstract class CombinedIngredient implements CustomIngredient {
    protected final List<Ingredient> ingredients;

    CombinedIngredient(List<Ingredient> ingredients) {
        if (ingredients.isEmpty()) {
            throw new IllegalArgumentException("ALL or ANY ingredient must have at least one sub-ingredient");
        }
        this.ingredients = List.copyOf(ingredients);
    }

    @Override public boolean requiresTesting() {
        return ingredients.stream().map(FabricIngredient.class::cast)
                .anyMatch(FabricIngredient::requiresTesting);
    }

    List<Ingredient> getIngredients() { return ingredients; }

    static final class Serializer<I extends CombinedIngredient>
            implements CustomIngredientSerializer<I> {
        private final ResourceLocation identifier;
        private final MapCodec<I> allowEmptyCodec;
        private final MapCodec<I> disallowEmptyCodec;
        private final StreamCodec<RegistryFriendlyByteBuf, I> packetCodec;

        Serializer(ResourceLocation identifier, Function<List<Ingredient>, I> factory,
                MapCodec<I> allowEmptyCodec, MapCodec<I> disallowEmptyCodec) {
            this.identifier = identifier;
            this.allowEmptyCodec = allowEmptyCodec;
            this.disallowEmptyCodec = disallowEmptyCodec;
            this.packetCodec = Ingredient.CONTENTS_STREAM_CODEC
                    .apply(ByteBufCodecs.list())
                    .map(factory, I::getIngredients);
        }

        @Override public ResourceLocation getIdentifier() { return identifier; }
        @Override public MapCodec<I> getCodec(boolean allowEmpty) {
            return allowEmpty ? allowEmptyCodec : disallowEmptyCodec;
        }
        @Override public StreamCodec<RegistryFriendlyByteBuf, I> getPacketCodec() {
            return packetCodec;
        }
    }
}
