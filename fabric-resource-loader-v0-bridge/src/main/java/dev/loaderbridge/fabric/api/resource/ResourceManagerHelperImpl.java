package dev.loaderbridge.fabric.api.resource;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.CompositePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

public final class ResourceManagerHelperImpl implements ResourceManagerHelper {
    private static final Map<PackType, ResourceManagerHelperImpl> HELPERS = new HashMap<>();
    private static final Map<PackType, List<BuiltinPack>> BUILTIN_PACKS =
            new EnumMap<>(PackType.class);
    private static final Pattern VALID_NAMESPACE = Pattern.compile("[a-z0-9._-]+");
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
        Objects.requireNonNull(subPath);
        List<Path> packRoots = new ArrayList<>();
        for (Path root : container.getRootPaths()) {
            Path normalizedRoot = root.toAbsolutePath().normalize();
            Path candidate = normalizedRoot.resolve(
                    subPath.replace("/", root.getFileSystem().getSeparator())).normalize();
            Path packRoot = candidate.startsWith(normalizedRoot)
                    ? SafePackPaths.containedDirectory(normalizedRoot, candidate) : null;
            if (packRoot != null) {
                packRoots.add(packRoot);
            }
        }
        if (packRoots.isEmpty()) return false;

        boolean registered = false;
        for (PackType type : PackType.values()) {
            if (!containsNamespace(packRoots, type)) continue;
            synchronized (BUILTIN_PACKS) {
                BUILTIN_PACKS.computeIfAbsent(type, ignored -> new ArrayList<>())
                        .add(new BuiltinPack(id, List.copyOf(packRoots), container,
                                displayName, activationType));
            }
            registered = true;
        }
        return registered;
    }

    static List<BuiltinPack> builtinPacks(PackType type) {
        synchronized (BUILTIN_PACKS) {
            return List.copyOf(BUILTIN_PACKS.getOrDefault(type, List.of()));
        }
    }

    static void loadBuiltinPacks(PackType type, Consumer<Pack> consumer) {
        for (BuiltinPack definition : builtinPacks(type)) {
            PackLocationInfo location = packLocation(definition);
            Pack.ResourcesSupplier resources = new Pack.ResourcesSupplier() {
                @Override
                public PackResources openPrimary(PackLocationInfo packLocation) {
                    return openBuiltinResources(definition, type, packLocation);
                }

                @Override
                public PackResources openFull(PackLocationInfo packLocation, Pack.Metadata metadata) {
                    PackResources primary = openPrimary(packLocation);
                    if (metadata.overlays().isEmpty()) return primary;
                    List<PackResources> overlays = metadata.overlays().stream()
                            .map(overlay -> ((FabricBuiltinPackResources) primary)
                                    .createOverlay(overlay))
                            .map(PackResources.class::cast)
                            .toList();
                    return new CompositePackResources(primary, overlays);
                }
            };
            Pack pack = Pack.readMetaAndCreate(location, resources, type,
                    packSelection(definition));
            if (pack != null) consumer.accept(pack);
        }
    }

    static PackLocationInfo packLocation(BuiltinPack definition) {
        PackSource source = PackSource.create(packName -> Component.translatable(
                "pack.nameAndSource", packName,
                Component.translatable("pack.source.builtinMod",
                        definition.container().getMetadata().getName())).withStyle(ChatFormatting.GRAY),
                definition.activationType().isEnabledByDefault());
        return new PackLocationInfo(definition.id().toString(), definition.displayName(),
                source, Optional.empty());
    }

    static PackSelectionConfig packSelection(BuiltinPack definition) {
        return new PackSelectionConfig(
                definition.activationType() == ResourcePackActivationType.ALWAYS_ENABLED,
                Pack.Position.TOP, false);
    }

    static PackResources openBuiltinResources(BuiltinPack definition, PackType type,
            PackLocationInfo location) {
        return new FabricBuiltinPackResources(location, definition.container(),
                definition.roots(), type);
    }

    private static boolean containsNamespace(List<Path> roots, PackType type) {
        for (Path root : roots) {
            Path typeRoot = root.resolve(type.getDirectory());
            if (!Files.isDirectory(typeRoot)) continue;
            try (DirectoryStream<Path> namespaces = Files.newDirectoryStream(typeRoot)) {
                for (Path namespace : namespaces) {
                    if (SafePackPaths.containedDirectory(root, namespace) != null
                            && VALID_NAMESPACE.matcher(namespace.getFileName().toString()).matches()) {
                        return true;
                    }
                }
            } catch (IOException exception) {
                throw new IllegalStateException("LB-FAPI-RESOURCE-002: cannot inspect built-in pack "
                        + root, exception);
            }
        }
        return false;
    }

    record BuiltinPack(ResourceLocation id, List<Path> roots, ModContainer container,
            Component displayName, ResourcePackActivationType activationType) { }
}
