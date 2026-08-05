package dev.loaderbridge.fabric.api.biome.mixin;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Biome.class)
public interface BiomeInvoker {
    @Invoker("<init>")
    static Biome loaderbridge$create(Biome.ClimateSettings climate,
            BiomeSpecialEffects effects,
            BiomeGenerationSettings generation,
            MobSpawnSettings spawns) {
        throw new AssertionError("Mixin constructor invoker was not transformed");
    }
}
