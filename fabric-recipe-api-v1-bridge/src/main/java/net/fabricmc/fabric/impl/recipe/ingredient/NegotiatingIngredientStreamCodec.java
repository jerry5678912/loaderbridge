package net.fabricmc.fabric.impl.recipe.ingredient;

import java.util.Arrays;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.FabricIngredient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;

public final class NegotiatingIngredientStreamCodec
        implements StreamCodec<RegistryFriendlyByteBuf, Ingredient> {
    private static final System.Logger LOGGER = System.getLogger(
            NegotiatingIngredientStreamCodec.class.getName());
    private static final java.util.concurrent.atomic.AtomicBoolean FALLBACK_REPORTED =
            new java.util.concurrent.atomic.AtomicBoolean();
    private static final java.util.concurrent.atomic.AtomicBoolean CUSTOM_REPORTED =
            new java.util.concurrent.atomic.AtomicBoolean();
    private final StreamCodec<RegistryFriendlyByteBuf, Ingredient> fallback;

    public NegotiatingIngredientStreamCodec(StreamCodec<RegistryFriendlyByteBuf, Ingredient> fallback) {
        this.fallback = fallback;
    }

    @Override public Ingredient decode(RegistryFriendlyByteBuf buffer) {
        return fallback.decode(buffer);
    }

    @Override public void encode(RegistryFriendlyByteBuf buffer, Ingredient value) {
        CustomIngredient custom = ((FabricIngredient) value).getCustomIngredient();
        if (CustomIngredientNegotiation.shouldFallback(custom)) {
            if (FALLBACK_REPORTED.compareAndSet(false, true)) {
                LOGGER.log(System.Logger.Level.INFO,
                        "LoaderBridge encoded a Fabric custom ingredient as vanilla matching stacks");
            }
            fallback.encode(buffer, Ingredient.of(Arrays.stream(value.getItems())));
        } else {
            if (custom != null && CUSTOM_REPORTED.compareAndSet(false, true)) {
                LOGGER.log(System.Logger.Level.INFO,
                        "LoaderBridge encoded a negotiated Fabric custom ingredient natively");
            }
            fallback.encode(buffer, value);
        }
    }
}
