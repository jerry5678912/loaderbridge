package net.fabricmc.fabric.api.client.rendering.v1;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Queues Fabric model-layer definitions for Forge's layer-definition event. */
public final class EntityModelLayerRegistry {
    private static final Map<ModelLayerLocation, TexturedModelDataProvider> PROVIDERS =
            new LinkedHashMap<>();

    private EntityModelLayerRegistry() {
    }

    public static synchronized void registerModelLayer(ModelLayerLocation modelLayer,
            TexturedModelDataProvider provider) {
        Objects.requireNonNull(modelLayer, "modelLayer");
        Objects.requireNonNull(provider, "provider");
        if (PROVIDERS.putIfAbsent(modelLayer, provider) != null) {
            throw new IllegalArgumentException("Model layer " + modelLayer + " is already registered");
        }
    }

    public static synchronized void registerTo(
            EntityRenderersEvent.RegisterLayerDefinitions event) {
        PROVIDERS.forEach((layer, provider) ->
                event.registerLayerDefinition(layer, provider::createModelData));
    }

    public static synchronized ModelPart bakeRegistered(ModelLayerLocation modelLayer) {
        TexturedModelDataProvider provider = PROVIDERS.get(modelLayer);
        return provider == null ? null : provider.createModelData().bakeRoot();
    }

    @FunctionalInterface
    public interface TexturedModelDataProvider {
        LayerDefinition createModelData();
    }
}
