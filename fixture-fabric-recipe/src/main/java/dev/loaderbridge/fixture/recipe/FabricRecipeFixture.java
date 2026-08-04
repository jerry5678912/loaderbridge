package dev.loaderbridge.fixture.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.fabricmc.fabric.api.recipe.v1.ingredient.DefaultCustomIngredients;
import net.fabricmc.fabric.api.recipe.v1.ingredient.FabricIngredient;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

public final class FabricRecipeFixture implements ModInitializer {
    @Override
    public void onInitialize() {
        CustomIngredientSerializer.register(CountIngredient.SERIALIZER);

        Ingredient diamond = Ingredient.of(Items.DIAMOND);
        Ingredient stone = Ingredient.of(Items.STONE);
        assertMatch(DefaultCustomIngredients.all(diamond, Ingredient.of(Items.DIAMOND)),
                new ItemStack(Items.DIAMOND), true, "all");
        assertMatch(DefaultCustomIngredients.any(diamond, stone),
                new ItemStack(Items.STONE), true, "any");
        assertMatch(DefaultCustomIngredients.difference(
                        DefaultCustomIngredients.any(diamond, stone), stone),
                new ItemStack(Items.STONE), false, "difference");

        ItemStack namedDiamond = new ItemStack(Items.DIAMOND);
        namedDiamond.set(DataComponents.CUSTOM_NAME, Component.literal("LoaderBridge recipe fixture"));
        Ingredient components = DefaultCustomIngredients.components(namedDiamond);
        assertMatch(components, namedDiamond.copy(), true, "components");
        assertMatch(components, new ItemStack(Items.DIAMOND), false, "components mismatch");

        CompoundTag expectedData = new CompoundTag();
        expectedData.putString("loaderbridge", "recipe-fixture");
        ItemStack taggedDiamond = new ItemStack(Items.DIAMOND);
        taggedDiamond.set(DataComponents.CUSTOM_DATA, CustomData.of(expectedData));
        Ingredient customData = DefaultCustomIngredients.customData(diamond, expectedData);
        assertMatch(customData, taggedDiamond, true, "custom data");
        assertMatch(customData, new ItemStack(Items.DIAMOND), false, "custom data mismatch");

        Ingredient count = new CountIngredient(Ingredient.of(Items.COBBLESTONE), 2).toVanilla();
        ItemStack two = new ItemStack(Items.COBBLESTONE, 2);
        assertMatch(count, two, true, "custom serializer");
        if (!((FabricIngredient) count).requiresTesting()) {
            throw new IllegalStateException("Recipe fixture custom ingredient lost requiresTesting");
        }
        ServerLifecycleEvents.SERVER_STARTED.register(server -> verifyLoadedRecipe(server, "start"));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
            if (!success) throw new IllegalStateException("Recipe fixture data-pack reload failed");
            verifyLoadedRecipe(server, "reload");
        });
        System.out.println("LOADERBRIDGE_RECIPE_FIXTURE_READY serializers=6 builtins=5 custom=1");
    }

    private static void verifyLoadedRecipe(net.minecraft.server.MinecraftServer server, String phase) {
        CraftingInput input = CraftingInput.of(1, 1, List.of(new ItemStack(Items.COBBLESTONE)));
        var match = server.getRecipeManager().getRecipeFor(
                RecipeType.CRAFTING, input, server.overworld()).orElseThrow(
                () -> new IllegalStateException("Recipe fixture custom JSON recipe did not match"));
        ResourceLocation expected = ResourceLocation.fromNamespaceAndPath(
                "loaderbridge_recipe_fixture", "any_to_diamond");
        if (!match.id().equals(expected)) {
            throw new IllegalStateException("Recipe fixture matched unexpected recipe " + match.id());
        }
        ItemStack result = match.value().assemble(input, server.registryAccess());
        if (!result.is(Items.DIAMOND) || result.getCount() != 1) {
            throw new IllegalStateException("Recipe fixture produced unexpected result " + result);
        }
        System.out.println("LOADERBRIDGE_RECIPE_RESOURCE_MATCH_READY phase=" + phase
                + " recipe=" + match.id() + " result=minecraft:diamond");
    }

    private static void assertMatch(Ingredient ingredient, ItemStack stack,
            boolean expected, String label) {
        if (ingredient.test(stack) != expected) {
            throw new IllegalStateException("Recipe fixture " + label + " assertion failed");
        }
    }

    public record CountIngredient(Ingredient base, int count) implements CustomIngredient {
        private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
                "loaderbridge_recipe_fixture", "count");
        public static final CustomIngredientSerializer<CountIngredient> SERIALIZER =
                new CustomIngredientSerializer<>() {
                    private final MapCodec<CountIngredient> codec = RecordCodecBuilder.mapCodec(instance ->
                            instance.group(
                                    Ingredient.CODEC.fieldOf("base").forGetter(CountIngredient::base),
                                    Codec.INT.fieldOf("count").forGetter(CountIngredient::count))
                                    .apply(instance, CountIngredient::new));
                    private final StreamCodec<RegistryFriendlyByteBuf, CountIngredient> packetCodec =
                            StreamCodec.composite(
                                    Ingredient.CONTENTS_STREAM_CODEC, CountIngredient::base,
                                    net.minecraft.network.codec.ByteBufCodecs.VAR_INT, CountIngredient::count,
                                    CountIngredient::new);

                    @Override public ResourceLocation getIdentifier() { return ID; }
                    @Override public MapCodec<CountIngredient> getCodec(boolean allowEmpty) { return codec; }
                    @Override public StreamCodec<RegistryFriendlyByteBuf, CountIngredient> getPacketCodec() {
                        return packetCodec;
                    }
                };

        @Override public boolean test(ItemStack stack) {
            return base.test(stack) && stack.getCount() >= count;
        }
        @Override public List<ItemStack> getMatchingStacks() {
            return List.of(new ItemStack(Items.COBBLESTONE, count));
        }
        @Override public boolean requiresTesting() { return true; }
        @Override public CustomIngredientSerializer<?> getSerializer() { return SERIALIZER; }
    }
}
