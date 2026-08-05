package net.fabricmc.fabric.api.resource;

import dev.loaderbridge.fabric.api.resource.ResourceManagerHelperImpl;
import java.util.function.Function;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

public interface ResourceManagerHelper {
    @Deprecated
    default void addReloadListener(IdentifiableResourceReloadListener listener) {
        registerReloadListener(listener);
    }

    void registerReloadListener(IdentifiableResourceReloadListener listener);

    void registerReloadListener(ResourceLocation identifier,
            Function<RegistryAccess.Frozen, IdentifiableResourceReloadListener> listenerFactory);

    static ResourceManagerHelper get(PackType type) {
        return ResourceManagerHelperImpl.get(type);
    }

    static boolean registerBuiltinResourcePack(ResourceLocation id, ModContainer container,
            ResourcePackActivationType activationType) {
        return ResourceManagerHelperImpl.registerBuiltinResourcePack(id,
                "resourcepacks/" + id.getPath(), container,
                Component.literal(id.getNamespace() + "/" + id.getPath()), activationType);
    }

    static boolean registerBuiltinResourcePack(ResourceLocation id, ModContainer container,
            Component displayName, ResourcePackActivationType activationType) {
        return ResourceManagerHelperImpl.registerBuiltinResourcePack(id,
                "resourcepacks/" + id.getPath(), container, displayName, activationType);
    }

    @Deprecated
    static boolean registerBuiltinResourcePack(ResourceLocation id, ModContainer container,
            String displayName, ResourcePackActivationType activationType) {
        return registerBuiltinResourcePack(id, container, Component.literal(displayName), activationType);
    }

    @Deprecated
    static boolean registerBuiltinResourcePack(ResourceLocation id, String subPath,
            ModContainer container, boolean enabledByDefault) {
        return ResourceManagerHelperImpl.registerBuiltinResourcePack(id, subPath, container,
                Component.literal(id.getNamespace() + "/" + id.getPath()), enabledByDefault
                        ? ResourcePackActivationType.DEFAULT_ENABLED : ResourcePackActivationType.NORMAL);
    }
}
