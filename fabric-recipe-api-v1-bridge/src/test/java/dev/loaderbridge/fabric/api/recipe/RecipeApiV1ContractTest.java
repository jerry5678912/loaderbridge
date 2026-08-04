package dev.loaderbridge.fabric.api.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.fabricmc.fabric.api.recipe.v1.ingredient.DefaultCustomIngredients;
import net.fabricmc.fabric.api.recipe.v1.ingredient.FabricIngredient;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.Test;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

class RecipeApiV1ContractTest {
    @Test
    void providerPinsExactPublicContractAndDynamicDependency() {
        var descriptor = new FabricRecipeApiV1BridgeProvider().descriptor();
        assertThat(descriptor.contractVersion()).isEqualTo("fabric-recipe-api-v1:5.0.16");
        assertThat(descriptor.implementationVersion())
                .isEqualTo("5.0.16+2475392c19-loaderbridge.1");
        assertThat(descriptor.providedModVersions())
                .containsEntry("fabric-recipe-api-v1", "5.0.16+2475392c19");
        assertThat(descriptor.requiredModules()).containsExactly("fabric-networking-api-v1-bridge");
        assertThat(descriptor.providedClasses()).hasSize(4);
    }

    @Test
    void exposesEveryPinnedPublicMethod() {
        assertMethods(CustomIngredient.class,
                signature("test", ItemStack.class),
                signature("getMatchingStacks"),
                signature("requiresTesting"),
                signature("getSerializer"),
                signature("toVanilla"));
        assertMethods(CustomIngredientSerializer.class,
                signature("register", CustomIngredientSerializer.class),
                signature("get", net.minecraft.resources.ResourceLocation.class),
                signature("getIdentifier"),
                signature("getCodec", boolean.class),
                signature("getPacketCodec"));
        assertMethods(DefaultCustomIngredients.class,
                signature("all", Ingredient[].class),
                signature("any", Ingredient[].class),
                signature("difference", Ingredient.class, Ingredient.class),
                signature("components", Ingredient.class, DataComponentPatch.class),
                signature("components", Ingredient.class, java.util.function.UnaryOperator.class),
                signature("components", ItemStack.class),
                signature("customData", Ingredient.class, CompoundTag.class));
        assertMethods(FabricIngredient.class,
                signature("getCustomIngredient"), signature("requiresTesting"));
    }

    @Test
    void combinedFactoriesRejectEmptyInputsLikeFabric() {
        assertThatThrownBy(DefaultCustomIngredients::all)
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(DefaultCustomIngredients::any)
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negotiationFallsBackOnlyWhenPeerDoesNotAdvertiseSerializer() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("loaderbridge", "contract");
        CustomIngredientSerializer<CustomIngredient> serializer = new CustomIngredientSerializer<>() {
            @Override public ResourceLocation getIdentifier() { return id; }
            @Override public com.mojang.serialization.MapCodec<CustomIngredient> getCodec(
                    boolean allowEmpty) { throw new UnsupportedOperationException(); }
            @Override public StreamCodec<RegistryFriendlyByteBuf, CustomIngredient> getPacketCodec() {
                throw new UnsupportedOperationException();
            }
        };
        CustomIngredient ingredient = new CustomIngredient() {
            @Override public boolean test(ItemStack stack) { return false; }
            @Override public List<ItemStack> getMatchingStacks() { return List.of(); }
            @Override public boolean requiresTesting() { return true; }
            @Override public CustomIngredientSerializer<?> getSerializer() { return serializer; }
        };

        var current = net.fabricmc.fabric.impl.recipe.ingredient.CustomIngredientNegotiation
                .CURRENT_SUPPORTED_SERIALIZERS;
        current.remove();
        assertThat(net.fabricmc.fabric.impl.recipe.ingredient.CustomIngredientNegotiation
                .shouldFallback(ingredient)).isFalse();
        current.set(java.util.Set.of());
        assertThat(net.fabricmc.fabric.impl.recipe.ingredient.CustomIngredientNegotiation
                .shouldFallback(ingredient)).isTrue();
        current.set(java.util.Set.of(id));
        assertThat(net.fabricmc.fabric.impl.recipe.ingredient.CustomIngredientNegotiation
                .shouldFallback(ingredient)).isFalse();
        current.remove();
    }

    private static void assertMethods(Class<?> type, String... expected) {
        assertThat(List.of(type.getDeclaredMethods()).stream().map(RecipeApiV1ContractTest::signature))
                .contains(expected);
    }

    private static String signature(String name, Class<?>... parameters) {
        return name + List.of(parameters);
    }

    private static String signature(Method method) {
        return signature(method.getName(), method.getParameterTypes());
    }
}
