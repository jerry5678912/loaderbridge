package net.fabricmc.fabric.api.transfer.v1.fluid;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Fabric-compatible common fluid attributes with validated handler results. */
public final class FluidVariantAttributes {
    private static final Logger LOGGER = LoggerFactory.getLogger("fabric-transfer-api-v1/fluid-attributes");
    private static final Map<Fluid, FluidVariantAttributeHandler> HANDLERS =
            new ConcurrentHashMap<>();
    private static final FluidVariantAttributeHandler DEFAULT_HANDLER =
            new FluidVariantAttributeHandler() { };
    private static volatile boolean coloredVanillaFluidNames;

    static {
        register(Fluids.WATER, new FluidVariantAttributeHandler() {
            @Override public Component getName(FluidVariant variant) {
                return coloredVanillaFluidNames
                        ? Blocks.WATER.getName().copy().withStyle(ChatFormatting.BLUE)
                        : FluidVariantAttributeHandler.super.getName(variant);
            }

            @Override public Optional<SoundEvent> getEmptySound(FluidVariant variant) {
                return Optional.of(SoundEvents.BUCKET_EMPTY);
            }
        });
        register(Fluids.LAVA, new FluidVariantAttributeHandler() {
            @Override public Component getName(FluidVariant variant) {
                return coloredVanillaFluidNames
                        ? Blocks.LAVA.getName().copy().withStyle(ChatFormatting.RED)
                        : FluidVariantAttributeHandler.super.getName(variant);
            }

            @Override public Optional<SoundEvent> getFillSound(FluidVariant variant) {
                return Optional.of(SoundEvents.BUCKET_FILL_LAVA);
            }

            @Override public Optional<SoundEvent> getEmptySound(FluidVariant variant) {
                return Optional.of(SoundEvents.BUCKET_EMPTY_LAVA);
            }

            @Override public int getTemperature(FluidVariant variant) {
                return FluidConstants.LAVA_TEMPERATURE;
            }

            @Override public int getViscosity(FluidVariant variant, Level world) {
                return world != null && world.dimensionType().ultraWarm()
                        ? FluidConstants.LAVA_VISCOSITY_NETHER
                        : FluidConstants.LAVA_VISCOSITY;
            }
        });
    }

    public static void register(Fluid fluid, FluidVariantAttributeHandler handler) {
        if (HANDLERS.putIfAbsent(fluid, handler) != null) {
            throw new IllegalArgumentException("Duplicate handler registration for fluid " + fluid);
        }
    }

    public static void enableColoredVanillaFluidNames() {
        coloredVanillaFluidNames = true;
    }

    public static FluidVariantAttributeHandler getHandler(Fluid fluid) {
        return HANDLERS.get(fluid);
    }

    public static FluidVariantAttributeHandler getHandlerOrDefault(Fluid fluid) {
        return HANDLERS.getOrDefault(fluid, DEFAULT_HANDLER);
    }

    public static Component getName(FluidVariant variant) {
        return getHandlerOrDefault(variant.getFluid()).getName(variant);
    }

    public static SoundEvent getFillSound(FluidVariant variant) {
        return getHandlerOrDefault(variant.getFluid()).getFillSound(variant)
                .or(() -> variant.getFluid().getPickupSound())
                .orElse(SoundEvents.BUCKET_FILL);
    }

    public static SoundEvent getEmptySound(FluidVariant variant) {
        return getHandlerOrDefault(variant.getFluid()).getEmptySound(variant)
                .orElse(SoundEvents.BUCKET_EMPTY);
    }

    public static int getLuminance(FluidVariant variant) {
        int value = getHandlerOrDefault(variant.getFluid()).getLuminance(variant);
        if (value >= 0 && value <= 15) return value;
        LOGGER.warn("Broken FluidVariantAttributeHandler. Invalid luminance {} for fluid variant {}",
                value, variant);
        return DEFAULT_HANDLER.getLuminance(variant);
    }

    public static int getTemperature(FluidVariant variant) {
        int value = getHandlerOrDefault(variant.getFluid()).getTemperature(variant);
        if (value >= 0) return value;
        LOGGER.warn("Broken FluidVariantAttributeHandler. Invalid temperature {} for fluid variant {}",
                value, variant);
        return DEFAULT_HANDLER.getTemperature(variant);
    }

    public static int getViscosity(FluidVariant variant, Level world) {
        int value = getHandlerOrDefault(variant.getFluid()).getViscosity(variant, world);
        if (value > 0) return value;
        LOGGER.warn("Broken FluidVariantAttributeHandler. Invalid viscosity {} for fluid variant {}",
                value, variant);
        return DEFAULT_HANDLER.getViscosity(variant, world);
    }

    public static boolean isLighterThanAir(FluidVariant variant) {
        return getHandlerOrDefault(variant.getFluid()).isLighterThanAir(variant);
    }

    private FluidVariantAttributes() { }
}
