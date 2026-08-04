package net.fabricmc.fabric.impl.recipe.ingredient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.fabricmc.fabric.api.recipe.v1.ingredient.FabricIngredient;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.crafting.ingredients.AbstractIngredient;
import net.minecraftforge.common.crafting.ingredients.IIngredientSerializer;
import org.jetbrains.annotations.Nullable;

public final class CustomIngredientImpl extends AbstractIngredient implements FabricIngredient {
    public static final String TYPE_KEY = "fabric:type";
    static final Map<ResourceLocation, CustomIngredientSerializer<?>> REGISTERED_SERIALIZERS =
            new ConcurrentHashMap<>();
    public static final Codec<CustomIngredientSerializer<?>> CODEC = ResourceLocation.CODEC.flatXmap(
            identifier -> Optional.ofNullable(REGISTERED_SERIALIZERS.get(identifier))
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(
                            () -> "Unknown custom ingredient serializer: " + identifier)),
            serializer -> DataResult.success(serializer.getIdentifier()));

    private final CustomIngredient customIngredient;
    private ItemStack[] matchingStacks;

    public CustomIngredientImpl(CustomIngredient customIngredient) {
        this.customIngredient = Objects.requireNonNull(customIngredient, "Custom ingredient cannot be null");
    }

    public static void registerSerializer(CustomIngredientSerializer<?> serializer) {
        Objects.requireNonNull(serializer, "CustomIngredientSerializer may not be null");
        ResourceLocation identifier = Objects.requireNonNull(serializer.getIdentifier(),
                "CustomIngredientSerializer identifier may not be null");
        if (REGISTERED_SERIALIZERS.putIfAbsent(identifier, serializer) != null) {
            throw new IllegalArgumentException(
                    "CustomIngredientSerializer with identifier " + identifier + " already registered.");
        }
    }

    @Nullable
    public static CustomIngredientSerializer<?> getSerializer(ResourceLocation identifier) {
        return REGISTERED_SERIALIZERS.get(Objects.requireNonNull(identifier, "Identifier may not be null"));
    }

    public static java.util.Set<ResourceLocation> registeredSerializerIds() {
        return java.util.Set.copyOf(REGISTERED_SERIALIZERS.keySet());
    }

    @Override public CustomIngredient getCustomIngredient() { return customIngredient; }

    @Override public boolean requiresTesting() { return customIngredient.requiresTesting(); }

    @Override public ItemStack[] getItems() {
        if (matchingStacks == null || checkInvalidation()) {
            markValid();
            matchingStacks = customIngredient.getMatchingStacks().toArray(ItemStack[]::new);
        }
        return matchingStacks;
    }

    @Override public boolean test(@Nullable ItemStack stack) {
        return stack != null && customIngredient.test(stack);
    }

    @Override public boolean isEmpty() {
        return matchingStacks != null && matchingStacks.length == 0;
    }

    @Override public boolean isSimple() { return !requiresTesting(); }

    @Override public IIngredientSerializer<? extends net.minecraft.world.item.crafting.Ingredient> serializer() {
        return FabricCustomIngredientForgeSerializer.INSTANCE;
    }

    @Override protected void invalidate() {
        super.invalidate();
        matchingStacks = null;
    }
}
