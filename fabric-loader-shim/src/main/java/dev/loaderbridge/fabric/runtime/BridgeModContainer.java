package dev.loaderbridge.fabric.runtime;

import dev.loaderbridge.fabric.metadata.FabricDependencies;
import dev.loaderbridge.fabric.metadata.FabricModMetadata;
import dev.loaderbridge.fabric.metadata.FabricVersionPredicate;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.ModEnvironment;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.fabricmc.loader.api.metadata.version.VersionInterval;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;

public record BridgeModContainer(
        ModMetadata metadata,
        List<Path> rootPaths,
        String parentModId,
        String parentSubLocation) implements ModContainer {
    public BridgeModContainer {
        rootPaths = List.copyOf(rootPaths);
        if ((parentModId == null) != (parentSubLocation == null)) {
            throw new IllegalArgumentException("Nested origin requires both parent mod ID and sub-location");
        }
    }

    public static BridgeModContainer create(String id, String version, String name,
            Collection<String> aliases, Path root) {
        Version parsedVersion = new SimpleVersion(version);
        ModMetadata metadata = new SimpleMetadata(
                id, aliases, parsedVersion, name, ModEnvironment.UNIVERSAL, List.of());
        return new BridgeModContainer(metadata, List.of(root), null, null);
    }

    public static BridgeModContainer create(FabricModMetadata source, Path root) {
        return create(source, root, null, null);
    }

    public static BridgeModContainer create(FabricModMetadata source, Path root,
            String parentModId, String parentSubLocation) {
        ModMetadata metadata = new SimpleMetadata(
                source.id(),
                source.provides(),
                new SimpleVersion(source.version()),
                source.name(),
                parseEnvironment(source.environment()),
                dependencies(source.dependencies()));
        return new BridgeModContainer(metadata, List.of(root), parentModId, parentSubLocation);
    }

    private static ModEnvironment parseEnvironment(String environment) {
        return switch (environment) {
            case "client" -> ModEnvironment.CLIENT;
            case "server" -> ModEnvironment.SERVER;
            default -> ModEnvironment.UNIVERSAL;
        };
    }

    private static List<ModDependency> dependencies(FabricDependencies source) {
        List<ModDependency> result = new ArrayList<>();
        addDependencies(result, ModDependency.Kind.DEPENDS, source.depends());
        addDependencies(result, ModDependency.Kind.RECOMMENDS, source.recommends());
        addDependencies(result, ModDependency.Kind.SUGGESTS, source.suggests());
        addDependencies(result, ModDependency.Kind.BREAKS, source.breaks());
        addDependencies(result, ModDependency.Kind.CONFLICTS, source.conflicts());
        return List.copyOf(result);
    }

    private static void addDependencies(List<ModDependency> target, ModDependency.Kind kind,
            Map<String, List<String>> dependencies) {
        dependencies.forEach((id, ranges) -> target.add(new BridgeDependency(kind, id, ranges)));
    }

    @Override public ModMetadata getMetadata() { return metadata; }
    @Override public List<Path> getRootPaths() { return rootPaths; }
    @Override public ModOrigin getOrigin() {
        if (parentModId != null) {
            return new ModOrigin() {
                @Override public Kind getKind() { return Kind.NESTED; }
                @Override public List<Path> getPaths() {
                    throw new UnsupportedOperationException("kind NESTED doesn't have paths");
                }
                @Override public String getParentModId() { return parentModId; }
                @Override public String getParentSubLocation() { return parentSubLocation; }
            };
        }
        return new ModOrigin() {
            @Override public Kind getKind() { return Kind.PATH; }
            @Override public List<Path> getPaths() { return rootPaths; }
            @Override public String getParentModId() {
                throw new UnsupportedOperationException("kind PATH doesn't have a parent mod");
            }
            @Override public String getParentSubLocation() {
                throw new UnsupportedOperationException("kind PATH doesn't have a parent sub-location");
            }
        };
    }
    @Override public java.util.Optional<ModContainer> getContainingMod() {
        return parentModId == null
                ? java.util.Optional.empty()
                : BridgeFabricLoader.getInstance().getModContainer(parentModId);
    }
    @Override public Collection<ModContainer> getContainedMods() {
        return BridgeFabricLoader.getInstance().getAllMods().stream()
                .filter(BridgeModContainer.class::isInstance)
                .map(BridgeModContainer.class::cast)
                .filter(container -> metadata.getId().equals(container.parentModId()))
                .map(ModContainer.class::cast)
                .toList();
    }
    @Override @Deprecated public Path getRootPath() { return rootPaths.getFirst(); }
    @Override @Deprecated public Path getPath(String file) { return getRootPath().resolve(file); }

    private record SimpleMetadata(
            String id,
            Collection<String> provides,
            Version version,
            String name,
            ModEnvironment environment,
            Collection<ModDependency> dependencies)
            implements ModMetadata {
        SimpleMetadata {
            provides = List.copyOf(provides);
            dependencies = List.copyOf(dependencies);
        }
        @Override public String getId() { return id; }
        @Override public Collection<String> getProvides() { return provides; }
        @Override public Version getVersion() { return version; }
        @Override public String getName() { return name; }
        @Override public ModEnvironment getEnvironment() { return environment; }
        @Override public Collection<ModDependency> getDependencies() { return dependencies; }
    }

    private record BridgeDependency(Kind kind, String modId, List<String> ranges) implements ModDependency {
        BridgeDependency { ranges = List.copyOf(ranges); }
        @Override public Kind getKind() { return kind; }
        @Override public String getModId() { return modId; }
        @Override public boolean matches(Version version) {
            return FabricVersionPredicate.anyMatches(ranges, version.getFriendlyString());
        }
        @Override public Collection<VersionPredicate> getVersionRequirements() { return List.of(); }
        @Override public List<VersionInterval> getVersionIntervals() { return List.of(); }
    }

    private record SimpleVersion(String friendlyString) implements Version {
        @Override public String getFriendlyString() { return friendlyString; }
        @Override public int compareTo(Version other) {
            return friendlyString.compareTo(other.getFriendlyString());
        }
        @Override public String toString() { return friendlyString; }
    }
}
