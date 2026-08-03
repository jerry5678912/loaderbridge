package net.fabricmc.fabric.api.object.builder.v1.entity;

import dev.loaderbridge.fabric.api.object.builder.DefaultAttributeBridge;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

/** Binary-compatible registration facade backed by Forge's attribute creation event. */
public final class FabricDefaultAttributeRegistry {
    private FabricDefaultAttributeRegistry() {
    }

    public static void register(EntityType<? extends LivingEntity> type,
            AttributeSupplier.Builder builder) {
        register(type, builder.build());
    }

    public static void register(EntityType<? extends LivingEntity> type,
            AttributeSupplier attributes) {
        DefaultAttributeBridge.register(type, attributes);
    }
}
