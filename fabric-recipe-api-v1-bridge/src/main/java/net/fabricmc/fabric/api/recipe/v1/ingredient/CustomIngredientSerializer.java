package net.fabricmc.fabric.api.recipe.v1.ingredient;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.impl.recipe.ingredient.CustomIngredientImpl;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Serializer contract used by Fabric custom ingredients. */
public interface CustomIngredientSerializer<T extends CustomIngredient> {
    static void register(CustomIngredientSerializer<?> serializer) {
        CustomIngredientImpl.registerSerializer(serializer);
    }

    @Nullable
    static CustomIngredientSerializer<?> get(ResourceLocation identifier) {
        return CustomIngredientImpl.getSerializer(identifier);
    }

    ResourceLocation getIdentifier();

    MapCodec<T> getCodec(boolean allowEmpty);

    StreamCodec<RegistryFriendlyByteBuf, T> getPacketCodec();
}
