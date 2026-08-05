package net.fabricmc.fabric.api.transfer.v1.fluid;

import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Common server-safe attributes for variants of a fluid. */
public interface FluidVariantAttributeHandler {
    default Component getName(FluidVariant fluidVariant) {
        var fluidBlock = fluidVariant.getFluid().defaultFluidState().createLegacyBlock().getBlock();
        if (!fluidVariant.isBlank() && fluidBlock == Blocks.AIR) {
            return Component.translatable(Util.makeDescriptionId(
                    "block", BuiltInRegistries.FLUID.getKey(fluidVariant.getFluid())));
        }
        return fluidBlock.getName();
    }

    default Optional<SoundEvent> getFillSound(FluidVariant variant) {
        return Optional.empty();
    }

    default Optional<SoundEvent> getEmptySound(FluidVariant variant) {
        return Optional.empty();
    }

    @SuppressWarnings("deprecation")
    default int getLuminance(FluidVariant variant) {
        return variant.getFluid().defaultFluidState().createLegacyBlock().getLightEmission();
    }

    default int getTemperature(FluidVariant variant) {
        return FluidConstants.WATER_TEMPERATURE;
    }

    default int getViscosity(FluidVariant variant, Level world) {
        return FluidConstants.WATER_VISCOSITY;
    }

    default boolean isLighterThanAir(FluidVariant variant) {
        return false;
    }
}
