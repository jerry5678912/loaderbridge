package dev.loaderbridge.fabric.runtime;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.ModOrigin;

public record BridgeModContainer(ModMetadata metadata, List<Path> rootPaths) implements ModContainer {
    public BridgeModContainer {
        rootPaths = List.copyOf(rootPaths);
    }

    public static BridgeModContainer create(String id, String version, String name,
            Collection<String> aliases, Path root) {
        Version parsedVersion = new SimpleVersion(version);
        ModMetadata metadata = new SimpleMetadata(id, aliases, parsedVersion, name);
        return new BridgeModContainer(metadata, List.of(root));
    }

    @Override public ModMetadata getMetadata() { return metadata; }
    @Override public List<Path> getRootPaths() { return rootPaths; }
    @Override public ModOrigin getOrigin() {
        return new ModOrigin() {
            @Override public Kind getKind() { return Kind.PATH; }
            @Override public List<Path> getPaths() { return rootPaths; }
            @Override public String getParentModId() { return null; }
            @Override public String getParentSubLocation() { return null; }
        };
    }
    @Override public java.util.Optional<ModContainer> getContainingMod() { return java.util.Optional.empty(); }
    @Override public Collection<ModContainer> getContainedMods() { return List.of(); }
    @Override @Deprecated public Path getRootPath() { return rootPaths.getFirst(); }
    @Override @Deprecated public Path getPath(String file) { return getRootPath().resolve(file); }

    private record SimpleMetadata(String id, Collection<String> provides, Version version, String name)
            implements ModMetadata {
        SimpleMetadata {
            provides = List.copyOf(provides);
        }
        @Override public String getId() { return id; }
        @Override public Collection<String> getProvides() { return provides; }
        @Override public Version getVersion() { return version; }
        @Override public String getName() { return name; }
    }

    private record SimpleVersion(String friendlyString) implements Version {
        @Override public String getFriendlyString() { return friendlyString; }
        @Override public int compareTo(Version other) {
            return friendlyString.compareTo(other.getFriendlyString());
        }
        @Override public String toString() { return friendlyString; }
    }
}
