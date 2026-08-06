package net.fabricmc.fabric.api.object.builder.v1.world.poi;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Creates and registers Fabric-compatible point-of-interest types. */
public final class PointOfInterestHelper {
    private PointOfInterestHelper() {}

    public static PoiType register(ResourceLocation id, int ticketCount, int searchDistance,
            Block... blocks) {
        ImmutableSet.Builder<BlockState> states = ImmutableSet.builder();
        for (Block block : blocks) {
            states.addAll(block.getStateDefinition().getPossibleStates());
        }
        return register(id, ticketCount, searchDistance, states.build());
    }

    public static PoiType register(ResourceLocation id, int ticketCount, int searchDistance,
            Iterable<BlockState> blocks) {
        return register(id, ticketCount, searchDistance,
                ImmutableSet.<BlockState>builder().addAll(blocks).build());
    }

    private static PoiType register(ResourceLocation id, int ticketCount, int searchDistance,
            Set<BlockState> states) {
        return Registry.register(BuiltInRegistries.POINT_OF_INTEREST_TYPE, id,
                new PoiType(states, ticketCount, searchDistance));
    }
}
