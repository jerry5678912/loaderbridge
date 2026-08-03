package dev.loaderbridge.fabric.api.biome;

import com.mojang.serialization.MapCodec;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod("loaderbridge_fabric_biome_api_v1")
public final class FabricBiomeBridgeMod {
    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,
                    "loaderbridge_fabric_biome_api_v1");

    static {
        SERIALIZERS.register("fabric_rules", () -> BridgeBiomeRules.CODEC);
    }

    @SuppressWarnings("removal")
    public FabricBiomeBridgeMod() {
        SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
