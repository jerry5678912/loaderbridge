package dev.loaderbridge.fabric.api.tag.convention;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public final class FabricTagKeySupport {
    private FabricTagKeySupport() { }

    public static String translationKey(TagKey<?> tagKey) {
        ResourceLocation registry = tagKey.registry().location();
        ResourceLocation tag = tagKey.location();
        StringBuilder key = new StringBuilder("tag.");
        if (!ResourceLocation.DEFAULT_NAMESPACE.equals(registry.getNamespace())) {
            key.append(registry.getNamespace()).append('.');
        }
        return key.append(registry.getPath().replace('/', '.')).append('.')
                .append(tag.getNamespace()).append('.')
                .append(tag.getPath().replace('/', '.').replace(':', '.')).toString();
    }

    public static Component name(TagKey<?> tagKey) {
        return Component.translatableWithFallback(translationKey(tagKey), "#" + tagKey.location());
    }
}
