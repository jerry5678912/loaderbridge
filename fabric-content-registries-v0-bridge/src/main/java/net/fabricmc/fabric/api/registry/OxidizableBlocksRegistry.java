package net.fabricmc.fabric.api.registry;

import dev.loaderbridge.fabric.api.content.registry.BridgeContentRegistries;
import java.util.Objects;
import net.minecraft.world.level.block.Block;

public final class OxidizableBlocksRegistry {
    private OxidizableBlocksRegistry() { }

    public static void registerOxidizableBlockPair(Block lessOxidized, Block moreOxidized) {
        BridgeContentRegistries.registerOxidizable(
                Objects.requireNonNull(lessOxidized, "Oxidizable block cannot be null!"),
                Objects.requireNonNull(moreOxidized, "Oxidizable block cannot be null!"));
    }

    public static void registerWaxableBlockPair(Block unwaxed, Block waxed) {
        BridgeContentRegistries.registerWaxable(
                Objects.requireNonNull(unwaxed, "Unwaxed block cannot be null!"),
                Objects.requireNonNull(waxed, "Waxed block cannot be null!"));
    }
}
