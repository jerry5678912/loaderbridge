package net.fabricmc.fabric.api.block.v1;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Block tags that opt into Fabric-provided behavior. */
public final class BlockFunctionalityTags {
    public static final TagKey<Block> CAN_CLIMB_TRAPDOOR_ABOVE =
            TagKey.create(Registries.BLOCK,
                    ResourceLocation.fromNamespaceAndPath("fabric", "can_climb_trapdoor_above"));

    private BlockFunctionalityTags() {
    }
}
