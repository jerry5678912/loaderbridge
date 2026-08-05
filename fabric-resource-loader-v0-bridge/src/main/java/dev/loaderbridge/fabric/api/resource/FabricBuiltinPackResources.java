package dev.loaderbridge.fabric.api.resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.resource.ModResourcePack;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.FileUtil;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;

final class FabricBuiltinPackResources extends AbstractPackResources implements ModResourcePack {
    private final ModContainer container;
    private final List<Path> roots;
    private final PackType registeredType;

    FabricBuiltinPackResources(PackLocationInfo location, ModContainer container,
            List<Path> roots, PackType registeredType) {
        super(location);
        this.container = container;
        this.roots = List.copyOf(roots);
        this.registeredType = registeredType;
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... pathSegments) {
        FileUtil.validatePath(pathSegments);
        return findFile(String.join("/", pathSegments));
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
        if (type != registeredType) return null;
        return findFile(type.getDirectory() + "/" + id.getNamespace() + "/" + id.getPath());
    }

    @Override
    public void listResources(PackType type, String namespace, String path,
            PackResources.ResourceOutput output) {
        if (type != registeredType || !getNamespaces(type).contains(namespace)) return;
        Set<ResourceLocation> emitted = new LinkedHashSet<>();
        for (Path root : roots) {
            Path namespaceRoot = SafePackPaths.containedDirectory(root,
                    root.resolve(type.getDirectory()).resolve(namespace));
            if (namespaceRoot == null) continue;
            Path searchRoot = namespaceRoot.resolve(path).normalize();
            if (!searchRoot.startsWith(namespaceRoot)) continue;
            searchRoot = SafePackPaths.containedDirectory(namespaceRoot, searchRoot);
            if (searchRoot == null) continue;
            try (var files = Files.walk(searchRoot)) {
                files.filter(Files::isRegularFile).forEach(file -> {
                    String relative = namespaceRoot.relativize(file).toString()
                            .replace(file.getFileSystem().getSeparator(), "/");
                    ResourceLocation id = ResourceLocation.tryBuild(namespace, relative);
                    Path safeFile = SafePackPaths.containedRegularFile(namespaceRoot, file);
                    if (id != null && safeFile != null && emitted.add(id)) {
                        output.accept(id, IoSupplier.create(safeFile));
                    }
                });
            } catch (IOException exception) {
                throw new IllegalStateException("LB-FAPI-RESOURCE-003: cannot list built-in pack "
                        + searchRoot, exception);
            }
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        if (type != registeredType) return Set.of();
        Set<String> namespaces = new LinkedHashSet<>();
        for (Path root : roots) {
            Path typeRoot = root.resolve(type.getDirectory());
            typeRoot = SafePackPaths.containedDirectory(root, typeRoot);
            if (typeRoot == null) continue;
            try (var children = Files.list(typeRoot)) {
                Path finalTypeRoot = typeRoot;
                children.filter(path -> SafePackPaths.containedDirectory(finalTypeRoot, path) != null)
                        .map(path -> path.getFileName().toString())
                        .filter(ResourceLocation::isValidNamespace)
                        .forEach(namespaces::add);
            } catch (IOException exception) {
                throw new IllegalStateException("LB-FAPI-RESOURCE-002: cannot inspect built-in pack "
                        + typeRoot, exception);
            }
        }
        return Set.copyOf(namespaces);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) throws IOException {
        T metadata = super.getMetadataSection(serializer);
        if (metadata != null || !"pack".equals(serializer.getMetadataSectionName())) return metadata;
        return (T) new PackMetadataSection(
                Component.literal(location().title().getString()),
                SharedConstants.getCurrentVersion().getPackVersion(registeredType), Optional.empty());
    }

    @Override
    public ModMetadata getFabricModMetadata() {
        return container.getMetadata();
    }

    @Override
    public ModResourcePack createOverlay(String overlay) {
        List<Path> overlayRoots = roots.stream()
                .map(root -> SafePackPaths.containedDirectory(root, root.resolve(overlay).normalize()))
                .filter(java.util.Objects::nonNull)
                .toList();
        return new FabricBuiltinPackResources(location(), container, overlayRoots, registeredType);
    }

    @Override public void close() { }

    private IoSupplier<InputStream> findFile(String relativePath) {
        for (Path root : roots) {
            Path normalizedRoot = root.toAbsolutePath().normalize();
            Path file = normalizedRoot.resolve(relativePath.replace(
                    "/", root.getFileSystem().getSeparator())).normalize();
            Path safeFile = file.startsWith(normalizedRoot)
                    ? SafePackPaths.containedRegularFile(normalizedRoot, file) : null;
            if (safeFile != null) {
                return IoSupplier.create(safeFile);
            }
        }
        return null;
    }
}
