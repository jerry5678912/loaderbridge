package dev.loaderbridge.fabric.api.recipe.mixin;

import java.util.Set;
import net.fabricmc.fabric.impl.recipe.ingredient.SupportedIngredientsConnection;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Connection.class)
public abstract class ConnectionMixin implements SupportedIngredientsConnection {
    @Unique private Set<ResourceLocation> loaderbridge$supportedCustomIngredients = Set.of();

    @Override public void loaderbridge$setSupportedCustomIngredients(
            Set<ResourceLocation> serializers) {
        loaderbridge$supportedCustomIngredients = Set.copyOf(serializers);
    }

    @Override public Set<ResourceLocation> loaderbridge$getSupportedCustomIngredients() {
        return loaderbridge$supportedCustomIngredients;
    }
}
