package net.fabricmc.fabric.api.registry;

import dev.loaderbridge.fabric.api.content.registry.BridgeContentRegistries;
import java.util.Objects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class FlattenableBlockRegistry {
    private FlattenableBlockRegistry() { }

    public static void register(Block input, BlockState flattened) {
        BridgeContentRegistries.registerFlattenable(
                Objects.requireNonNull(input, "input block cannot be null"),
                Objects.requireNonNull(flattened, "flattened block state cannot be null"));
    }
}
