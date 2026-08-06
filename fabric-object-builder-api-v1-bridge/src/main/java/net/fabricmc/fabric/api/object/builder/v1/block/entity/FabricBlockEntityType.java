package net.fabricmc.fabric.api.object.builder.v1.block.entity;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Fabric extensions injected into block-entity types and their vanilla builders. */
public interface FabricBlockEntityType {
    default void addSupportedBlock(Block block) {
        throw new AssertionError("Implemented by LoaderBridge Mixin");
    }

    interface Builder<T extends BlockEntity> {
        default BlockEntityType<T> build() {
            throw new AssertionError("Implemented by LoaderBridge Mixin");
        }
    }
}
