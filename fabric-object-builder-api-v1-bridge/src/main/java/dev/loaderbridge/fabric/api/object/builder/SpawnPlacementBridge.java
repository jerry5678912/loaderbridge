package dev.loaderbridge.fabric.api.object.builder;

import dev.loaderbridge.fabric.api.object.builder.mixin.SpawnPlacementsInvoker;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

/** Applies Fabric registrations to Minecraft's native placement table. */
public final class SpawnPlacementBridge {
    private SpawnPlacementBridge() {
    }

    public static <T extends Mob> void register(EntityType<T> type,
            SpawnPlacementType placement, Heightmap.Types heightmap,
            SpawnPlacements.SpawnPredicate<T> predicate) {
        SpawnPlacementsInvoker.loaderbridge$register(type, placement, heightmap, predicate);
    }
}
