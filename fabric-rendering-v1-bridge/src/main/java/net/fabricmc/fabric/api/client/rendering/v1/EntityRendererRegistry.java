package net.fabricmc.fabric.api.client.rendering.v1;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/** Fabric renderer registration backed by Minecraft's shared renderer registry. */
public final class EntityRendererRegistry {
    private EntityRendererRegistry() {
    }

    public static <E extends Entity> void register(EntityType<? extends E> entityType,
            EntityRendererProvider<E> entityRendererFactory) {
        EntityRenderers.register(entityType, entityRendererFactory);
    }
}
