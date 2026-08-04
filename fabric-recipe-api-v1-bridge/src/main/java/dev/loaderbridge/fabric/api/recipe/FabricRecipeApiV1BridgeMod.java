package dev.loaderbridge.fabric.api.recipe;

import net.fabricmc.fabric.impl.recipe.ingredient.CustomIngredientInit;
import net.fabricmc.fabric.impl.recipe.ingredient.CustomIngredientNegotiation;
import net.fabricmc.fabric.impl.recipe.ingredient.FabricCustomIngredientForgeSerializer;
import net.minecraftforge.common.crafting.ingredients.IIngredientSerializer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

@Mod("loaderbridge_fabric_recipe_api_v1")
public final class FabricRecipeApiV1BridgeMod {
    private static final DeferredRegister<IIngredientSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.INGREDIENT_SERIALIZERS,
                    "loaderbridge_fabric_recipe_api_v1");

    static {
        SERIALIZERS.register("fabric_custom", () -> FabricCustomIngredientForgeSerializer.INSTANCE);
    }

    @SuppressWarnings("removal")
    public FabricRecipeApiV1BridgeMod() {
        CustomIngredientInit.initialize();
        CustomIngredientNegotiation.initialize();
        SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.addListener(CustomIngredientNegotiation::gatherLoginTask);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> CustomIngredientNegotiation.initializeClient());
    }
}
