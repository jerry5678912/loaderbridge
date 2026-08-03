package net.fabricmc.fabric.api.registry;

import dev.loaderbridge.fabric.api.content.registry.BridgeContentRegistries;
import java.util.Objects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;

public final class StrippableBlockRegistry {
    private StrippableBlockRegistry() { }

    public static void register(Block input, Block stripped) {
        requireAxis(input, "input block");
        requireAxis(stripped, "stripped block");
        BridgeContentRegistries.registerStrippable(input, stripped);
    }

    private static void requireAxis(Block block, String name) {
        Objects.requireNonNull(block, name + " cannot be null");
        if (!block.getStateDefinition().getProperties().contains(RotatedPillarBlock.AXIS)) {
            throw new IllegalArgumentException(name + " must have an axis property");
        }
    }
}
