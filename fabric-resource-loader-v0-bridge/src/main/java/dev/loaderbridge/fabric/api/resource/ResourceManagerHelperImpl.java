package dev.loaderbridge.fabric.api.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

public final class ResourceManagerHelperImpl implements ResourceManagerHelper {
    private static final Map<PackType, ResourceManagerHelperImpl> HELPERS = new HashMap<>();
    private final PackType type;
    private final Map<ResourceLocation, Function<RegistryAccess.Frozen,
            IdentifiableResourceReloadListener>> factories = new LinkedHashMap<>();

    private ResourceManagerHelperImpl(PackType type) {
        this.type = type;
    }

    public static synchronized ResourceManagerHelperImpl get(PackType type) {
        return HELPERS.computeIfAbsent(Objects.requireNonNull(type), ResourceManagerHelperImpl::new);
    }

    @Override
    public synchronized void registerReloadListener(IdentifiableResourceReloadListener listener) {
        Objects.requireNonNull(listener);
        register(listener.getFabricId(), ignored -> listener);
    }

    @Override
    public synchronized void registerReloadListener(ResourceLocation identifier,
            Function<RegistryAccess.Frozen, IdentifiableResourceReloadListener> listenerFactory) {
        if (type == PackType.CLIENT_RESOURCES) {
            throw new IllegalArgumentException("Registry-aware listeners require SERVER_DATA");
        }
        register(identifier, listenerFactory);
    }

    private void register(ResourceLocation identifier,
            Function<RegistryAccess.Frozen, IdentifiableResourceReloadListener> factory) {
        Objects.requireNonNull(identifier);
        Objects.requireNonNull(factory);
        factories.putIfAbsent(identifier, factory);
    }

    synchronized List<IdentifiableResourceReloadListener> listeners(RegistryAccess.Frozen registries) {
        List<IdentifiableResourceReloadListener> pending = factories.entrySet().stream().map(entry -> {
            IdentifiableResourceReloadListener listener = Objects.requireNonNull(entry.getValue().apply(registries));
            if (!entry.getKey().equals(listener.getFabricId())) {
                throw new IllegalStateException("Listener factory for " + entry.getKey()
                        + " created listener " + listener.getFabricId());
            }
            return listener;
        }).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        List<IdentifiableResourceReloadListener> ordered = new ArrayList<>();
        Set<ResourceLocation> resolved = new HashSet<>();
        if (type == PackType.SERVER_DATA) {
            resolved.addAll(List.of(ResourceReloadListenerKeys.TAGS, ResourceReloadListenerKeys.RECIPES,
                    ResourceReloadListenerKeys.ADVANCEMENTS, ResourceReloadListenerKeys.FUNCTIONS));
        } else {
            resolved.addAll(List.of(ResourceReloadListenerKeys.SOUNDS, ResourceReloadListenerKeys.FONTS,
                    ResourceReloadListenerKeys.MODELS, ResourceReloadListenerKeys.LANGUAGES,
                    ResourceReloadListenerKeys.TEXTURES));
        }
        while (!pending.isEmpty()) {
            int size = pending.size();
            pending.removeIf(listener -> {
                if (!resolved.containsAll(listener.getFabricDependencies())) return false;
                resolved.add(listener.getFabricId());
                ordered.add(listener);
                return true;
            });
            if (pending.size() == size) {
                throw new IllegalStateException("LB-FAPI-RESOURCE-001: unresolved reload listener dependencies: "
                        + pending.stream().map(IdentifiableResourceReloadListener::getFabricId).toList());
            }
        }
        return List.copyOf(ordered);
    }

    public static boolean registerBuiltinResourcePack(ResourceLocation id, String subPath,
            ModContainer container, Component displayName, ResourcePackActivationType activationType) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(container);
        Objects.requireNonNull(displayName);
        Objects.requireNonNull(activationType);
        return false;
    }
}
