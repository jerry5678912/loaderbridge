package dev.loaderbridge.fabric.api.object.builder;

import dev.loaderbridge.fabric.api.object.builder.mixin.ForgeHooksAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

/** Transfers Fabric registrations into Forge's native attribute creation phase. */
public final class DefaultAttributeBridge {
    private DefaultAttributeBridge() {
    }

    public static void register(EntityType<? extends LivingEntity> type,
            AttributeSupplier attributes) {
        ForgeHooksAccessor.loaderbridge$getAttributes().put(type, attributes);
    }
}
