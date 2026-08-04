package dev.loaderbridge.fabric.api.recipe.mixin;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.recipe.v1.ingredient.FabricIngredient;
import net.fabricmc.fabric.impl.recipe.ingredient.FabricIngredientCodecs;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.fabricmc.fabric.impl.recipe.ingredient.NegotiatingIngredientStreamCodec;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Ingredient.class)
public abstract class IngredientMixin implements FabricIngredient {
    @Shadow @Final @Mutable private static Codec<Ingredient> CODEC;
    @Shadow @Final @Mutable private static Codec<Ingredient> CODEC_NONEMPTY;
    @Shadow @Final @Mutable
    private static StreamCodec<RegistryFriendlyByteBuf, Ingredient> CONTENTS_STREAM_CODEC;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void loaderbridge$addFabricCodecs(CallbackInfo callback) {
        CODEC = FabricIngredientCodecs.enhance(CODEC, true);
        CODEC_NONEMPTY = FabricIngredientCodecs.enhance(CODEC_NONEMPTY, false);
        CONTENTS_STREAM_CODEC = new NegotiatingIngredientStreamCodec(CONTENTS_STREAM_CODEC);
    }
}
