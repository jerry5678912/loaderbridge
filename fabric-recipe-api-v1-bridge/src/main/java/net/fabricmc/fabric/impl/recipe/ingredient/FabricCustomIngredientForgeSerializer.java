package net.fabricmc.fabric.impl.recipe.ingredient;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.ingredients.IIngredientSerializer;

public final class FabricCustomIngredientForgeSerializer
        implements IIngredientSerializer<CustomIngredientImpl> {
    public static final FabricCustomIngredientForgeSerializer INSTANCE =
            new FabricCustomIngredientForgeSerializer();

    @Override
    public MapCodec<CustomIngredientImpl> codec() {
        return FabricIngredientCodecs.customCodec(true)
                .xmap(CustomIngredientImpl::new, CustomIngredientImpl::getCustomIngredient);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(RegistryFriendlyByteBuf buffer, CustomIngredientImpl value) {
        CustomIngredient ingredient = value.getCustomIngredient();
        CustomIngredientSerializer<CustomIngredient> serializer =
                (CustomIngredientSerializer<CustomIngredient>) ingredient.getSerializer();
        buffer.writeResourceLocation(serializer.getIdentifier());
        serializer.getPacketCodec().encode(buffer, ingredient);
    }

    @Override
    public CustomIngredientImpl read(RegistryFriendlyByteBuf buffer) {
        ResourceLocation identifier = buffer.readResourceLocation();
        CustomIngredientSerializer<?> serializer = CustomIngredientSerializer.get(identifier);
        if (serializer == null) {
            throw new IllegalArgumentException(
                    "Cannot deserialize custom ingredient of unknown type " + identifier);
        }
        return new CustomIngredientImpl(serializer.getPacketCodec().decode(buffer));
    }

    private FabricCustomIngredientForgeSerializer() { }
}
