package dev.loaderbridge.fabric.api.object.builder;

import net.minecraft.world.entity.Entity;

/** Internal interface mixed into Minecraft's entity builder. */
public interface EntityTypeBuilderExtension<T extends Entity> {
    void loaderbridge$setBuildHook(EntityTypeExtensionBridge.BuildHook<T> hook);
}
