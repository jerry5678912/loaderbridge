package net.fabricmc.fabric.api.transfer.v1.fluid;

/** Standard Fabric fluid-transfer units and vanilla fluid attributes. */
public final class FluidConstants {
    public static final long BUCKET = 81_000;
    public static final long BOTTLE = 27_000;
    public static final long BOWL = 27_000;
    public static final long BLOCK = 81_000;
    public static final long INGOT = 9_000;
    public static final long NUGGET = 1_000;
    public static final long DROPLET = 1;
    public static final int WATER_TEMPERATURE = 300;
    public static final int LAVA_TEMPERATURE = 1_300;
    public static final int WATER_VISCOSITY = 1_000;
    public static final int LAVA_VISCOSITY = 6_000;
    public static final int LAVA_VISCOSITY_NETHER = 2_000;
    public static final int VISCOSITY_RATIO = 200;

    public static long fromBucketFraction(long numerator, long denominator) {
        long total = numerator * BUCKET;
        if (denominator == 0 || total % denominator != 0) {
            throw new IllegalArgumentException("Not a valid number of droplets!");
        }
        return total / denominator;
    }

    private FluidConstants() { }
}
