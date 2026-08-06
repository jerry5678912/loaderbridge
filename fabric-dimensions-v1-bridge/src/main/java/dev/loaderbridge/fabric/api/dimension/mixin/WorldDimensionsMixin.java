package dev.loaderbridge.fabric.api.dimension.mixin;

import com.mojang.datafixers.Products;
import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.loaderbridge.fabric.api.dimension.FailSoftMapCodec;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldDimensions.class)
public abstract class WorldDimensionsMixin {
    @Redirect(
            method = "lambda$static$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/serialization/codecs/RecordCodecBuilder$Instance;group(Lcom/mojang/datafixers/kinds/App;)Lcom/mojang/datafixers/Products$P1;"))
    private static Products.P1<RecordCodecBuilder.Mu<WorldDimensions>,
            Map<ResourceKey<LevelStem>, LevelStem>> loaderbridge$useSuccessfulFailSoftMap(
            RecordCodecBuilder.Instance<WorldDimensions> instance,
            App<RecordCodecBuilder.Mu<WorldDimensions>, ?> ignored) {
        return instance.group(new FailSoftMapCodec<>(
                ResourceKey.codec(Registries.LEVEL_STEM), LevelStem.CODEC)
                .fieldOf("dimensions").forGetter(WorldDimensions::dimensions));
    }
}
