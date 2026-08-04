package net.fabricmc.fabric.impl.recipe.ingredient.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.fabricmc.fabric.api.recipe.v1.ingredient.FabricIngredient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public final class DifferenceIngredient implements CustomIngredient {
    public static final CustomIngredientSerializer<DifferenceIngredient> SERIALIZER = new Serializer();
    private final Ingredient base;
    private final Ingredient subtracted;

    public DifferenceIngredient(Ingredient base, Ingredient subtracted) {
        this.base = base;
        this.subtracted = subtracted;
    }

    @Override public boolean test(ItemStack stack) {
        return base.test(stack) && !subtracted.test(stack);
    }

    @Override public List<ItemStack> getMatchingStacks() {
        List<ItemStack> stacks = new ArrayList<>(List.of(base.getItems()));
        stacks.removeIf(subtracted);
        return stacks;
    }

    @Override public boolean requiresTesting() {
        return ((FabricIngredient) base).requiresTesting()
                || ((FabricIngredient) subtracted).requiresTesting();
    }

    @Override public CustomIngredientSerializer<?> getSerializer() { return SERIALIZER; }
    private Ingredient getBase() { return base; }
    private Ingredient getSubtracted() { return subtracted; }

    private static final class Serializer implements CustomIngredientSerializer<DifferenceIngredient> {
        private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("fabric", "difference");
        private static final MapCodec<DifferenceIngredient> ALLOW_EMPTY_CODEC = createCodec(Ingredient.CODEC);
        private static final MapCodec<DifferenceIngredient> DISALLOW_EMPTY_CODEC = createCodec(Ingredient.CODEC_NONEMPTY);
        private static final StreamCodec<RegistryFriendlyByteBuf, DifferenceIngredient> PACKET_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, DifferenceIngredient::getBase,
                        Ingredient.CONTENTS_STREAM_CODEC, DifferenceIngredient::getSubtracted,
                        DifferenceIngredient::new);

        private static MapCodec<DifferenceIngredient> createCodec(Codec<Ingredient> codec) {
            return RecordCodecBuilder.mapCodec(instance -> instance.group(
                    codec.fieldOf("base").forGetter(DifferenceIngredient::getBase),
                    codec.fieldOf("subtracted").forGetter(DifferenceIngredient::getSubtracted))
                    .apply(instance, DifferenceIngredient::new));
        }

        @Override public ResourceLocation getIdentifier() { return ID; }
        @Override public MapCodec<DifferenceIngredient> getCodec(boolean allowEmpty) {
            return allowEmpty ? ALLOW_EMPTY_CODEC : DISALLOW_EMPTY_CODEC;
        }
        @Override public StreamCodec<RegistryFriendlyByteBuf, DifferenceIngredient> getPacketCodec() {
            return PACKET_CODEC;
        }
    }
}
