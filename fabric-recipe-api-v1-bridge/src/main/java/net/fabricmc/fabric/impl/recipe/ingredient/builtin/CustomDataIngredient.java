package net.fabricmc.fabric.impl.recipe.ingredient.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;

public final class CustomDataIngredient implements CustomIngredient {
    public static final CustomIngredientSerializer<CustomDataIngredient> SERIALIZER = new Serializer();
    private final Ingredient base;
    private final CompoundTag nbt;

    public CustomDataIngredient(Ingredient base, CompoundTag nbt) {
        if (nbt == null || nbt.isEmpty()) {
            throw new IllegalArgumentException(
                    "NBT cannot be null or empty; use components ingredient for strict matching");
        }
        this.base = Objects.requireNonNull(base, "Base ingredient cannot be null");
        this.nbt = nbt.copy();
    }

    @Override public boolean test(ItemStack stack) {
        if (!base.test(stack)) return false;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && customData.matchedBy(nbt);
    }

    @Override public List<ItemStack> getMatchingStacks() {
        List<ItemStack> stacks = new ArrayList<>(List.of(base.getItems()));
        stacks.replaceAll(stack -> {
            ItemStack copy = stack.copy();
            CustomData.update(DataComponents.CUSTOM_DATA, copy, tag -> tag.merge(nbt));
            return copy;
        });
        stacks.removeIf(stack -> !base.test(stack));
        return stacks;
    }

    @Override public boolean requiresTesting() { return true; }
    @Override public CustomIngredientSerializer<?> getSerializer() { return SERIALIZER; }
    private Ingredient getBase() { return base; }
    private CompoundTag getNbt() { return nbt; }

    private static final class Serializer implements CustomIngredientSerializer<CustomDataIngredient> {
        private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("fabric", "custom_data");
        private static final MapCodec<CustomDataIngredient> ALLOW_EMPTY_CODEC = createCodec(Ingredient.CODEC);
        private static final MapCodec<CustomDataIngredient> DISALLOW_EMPTY_CODEC = createCodec(Ingredient.CODEC_NONEMPTY);
        private static final StreamCodec<RegistryFriendlyByteBuf, CustomDataIngredient> PACKET_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, CustomDataIngredient::getBase,
                        ByteBufCodecs.COMPOUND_TAG, CustomDataIngredient::getNbt,
                        CustomDataIngredient::new);

        private static MapCodec<CustomDataIngredient> createCodec(Codec<Ingredient> codec) {
            return RecordCodecBuilder.mapCodec(instance -> instance.group(
                    codec.fieldOf("base").forGetter(CustomDataIngredient::getBase),
                    CompoundTag.CODEC.fieldOf("nbt").forGetter(CustomDataIngredient::getNbt))
                    .apply(instance, CustomDataIngredient::new));
        }

        @Override public ResourceLocation getIdentifier() { return ID; }
        @Override public MapCodec<CustomDataIngredient> getCodec(boolean allowEmpty) {
            return allowEmpty ? ALLOW_EMPTY_CODEC : DISALLOW_EMPTY_CODEC;
        }
        @Override public StreamCodec<RegistryFriendlyByteBuf, CustomDataIngredient> getPacketCodec() {
            return PACKET_CODEC;
        }
    }
}
