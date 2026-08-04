package dev.loaderbridge.fabric.runtime;

import dev.loaderbridge.fabric.metadata.FabricDependencies;
import dev.loaderbridge.fabric.metadata.FabricModMetadata;
import dev.loaderbridge.fabric.metadata.FabricPerson;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Comparator;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.ContactInformation;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModEnvironment;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.fabricmc.loader.api.metadata.Person;
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
        return create(id, version, name, aliases, "fabric", root);
    }

    public static BridgeModContainer createLoader(String version, Path root) {
        ModMetadata metadata = new SimpleMetadata(
                "fabric", "fabricloader", List.of(), parseVersion(version), "Fabric Loader",
                ModEnvironment.UNIVERSAL, List.of(), "The base mod loader.",
                List.of(new FabricPerson("FabricMC", Map.of())), List.of(),
                Map.of(
                        "homepage", "https://fabricmc.net",
                        "irc", "ircs://irc.esper.net:6697/fabric",
                        "issues", "https://github.com/FabricMC/fabric-loader/issues",
                        "sources", "https://github.com/FabricMC/fabric-loader"),
                List.of("Apache-2.0"), Map.of(0, "assets/fabricloader/icon.png"), Map.of());
        return new BridgeModContainer(metadata, List.of(root), null, null);
    }

    public static BridgeModContainer createBuiltin(String id, String version, String name,
            Path root) {
        return createBuiltin(id, version, name, root, Map.of());
    }

    public static BridgeModContainer createBuiltin(String id, String version, String name,
            Path root, Map<String, List<String>> requiredDependencies) {
        return createBuiltin(id, version, name, List.of(root), requiredDependencies);
    }

    public static BridgeModContainer createBuiltin(String id, String version, String name,
            Collection<Path> roots, Map<String, List<String>> requiredDependencies) {
        List<ModDependency> dependencies = new ArrayList<>();
        addDependencies(dependencies, ModDependency.Kind.DEPENDS, requiredDependencies);
        Version parsedVersion = parseVersion(version);
        ModMetadata metadata = new SimpleMetadata(
                "builtin", id, List.of(), parsedVersion, name, ModEnvironment.UNIVERSAL,
                dependencies, "", List.of(), List.of(), Map.of(), List.of(), Map.of(), Map.of());
        return new BridgeModContainer(metadata, List.copyOf(roots), null, null);
    }

    private static BridgeModContainer create(String id, String version, String name,
            Collection<String> aliases, String type, Path root) {
        return create(id, version, name, aliases, type, root, List.of());
    }

    private static BridgeModContainer create(String id, String version, String name,
            Collection<String> aliases, String type, Path root,
            Collection<ModDependency> dependencies) {
        Version parsedVersion = parseVersion(version);
        ModMetadata metadata = new SimpleMetadata(
                type, id, aliases, parsedVersion, name, ModEnvironment.UNIVERSAL, dependencies, "",
                List.of(), List.of(), Map.of(), List.of(), Map.of(), Map.of());
        return new BridgeModContainer(metadata, List.of(root), null, null);
    }

    public static BridgeModContainer create(FabricModMetadata source, Path root) {
        return create(source, root, null, null);
    }

    public static BridgeModContainer create(FabricModMetadata source, Path root,
            String parentModId, String parentSubLocation) {
        ModMetadata metadata = new SimpleMetadata(
                "fabric",
                source.id(),
                source.provides(),
                parseVersion(source.version()),
                source.name(),
                parseEnvironment(source.environment()),
                dependencies(source.dependencies()),
                source.description(), source.authors(), source.contributors(), source.contact(),
                source.licenses(), source.icons(), customValues(source.customJson()));
        return new BridgeModContainer(metadata, List.of(root), parentModId, parentSubLocation);
    }

    private static Map<String, CustomValue> customValues(Map<String, String> customJson) {
        Map<String, CustomValue> values = new LinkedHashMap<>();
        customJson.forEach((key, json) -> values.put(key, BridgeCustomValue.parse(json)));
        return Collections.unmodifiableMap(values);
    }

    private static ModEnvironment parseEnvironment(String environment) {
        return switch (environment) {
            case "client" -> ModEnvironment.CLIENT;
            case "server" -> ModEnvironment.SERVER;
            default -> ModEnvironment.UNIVERSAL;
        };
    }

    private static Version parseVersion(String value) {
        try {
            return Version.parse(value);
        } catch (net.fabricmc.loader.api.VersionParsingException exception) {
            throw new IllegalArgumentException("Invalid Fabric mod version: " + value, exception);
        }
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
                @Override public String toString() { return parentModId + ":" + parentSubLocation; }
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
            @Override public String toString() {
                return String.join(File.pathSeparator,
                        rootPaths.stream().map(Path::toString).toList());
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
    @Override @Deprecated public Path getPath(String file) {
        Optional<Path> existing = findPath(file);
        if (existing.isPresent()) return existing.get();
        if (!rootPaths.isEmpty()) {
            Path root = rootPaths.getFirst();
            return root.resolve(file.replace("/", root.getFileSystem().getSeparator()));
        }
        return Path.of(".").resolve("missing_ae236f4970ce")
                .resolve(file.replace('/', File.separatorChar));
    }
    @Override public boolean equals(Object other) { return this == other; }
    @Override public int hashCode() { return System.identityHashCode(this); }
    @Override public String toString() {
        return metadata.getId() + " " + metadata.getVersion();
    }

    private record SimpleMetadata(
            String type,
            String id,
            Collection<String> provides,
            Version version,
            String name,
            ModEnvironment environment,
            Collection<ModDependency> dependencies,
            String description,
            List<FabricPerson> authors,
            List<FabricPerson> contributors,
            Map<String, String> contact,
            List<String> licenses,
            Map<Integer, String> icons,
            Map<String, CustomValue> customValues)
            implements ModMetadata {
        SimpleMetadata {
            provides = List.copyOf(provides);
            dependencies = List.copyOf(dependencies);
            authors = List.copyOf(authors);
            contributors = List.copyOf(contributors);
            contact = Map.copyOf(contact);
            licenses = List.copyOf(licenses);
            icons = Map.copyOf(icons);
            customValues = Collections.unmodifiableMap(new LinkedHashMap<>(customValues));
        }
        @Override public String getType() { return type; }
        @Override public String getId() { return id; }
        @Override public Collection<String> getProvides() { return provides; }
        @Override public Version getVersion() { return version; }
        @Override public String getName() { return name; }
        @Override public ModEnvironment getEnvironment() { return environment; }
        @Override public Collection<ModDependency> getDependencies() { return dependencies; }
        @Override public String getDescription() { return description; }
        @Override public Collection<Person> getAuthors() {
            return authors.stream().<Person>map(SimplePerson::new).toList();
        }
        @Override public Collection<Person> getContributors() {
            return contributors.stream().<Person>map(SimplePerson::new).toList();
        }
        @Override public ContactInformation getContact() { return new SimpleContact(contact); }
        @Override public Collection<String> getLicense() { return licenses; }
        @Override public Optional<String> getIconPath(int size) {
            if (icons.isEmpty()) return Optional.empty();
            if (icons.containsKey(0)) return Optional.of(icons.get(0));
            return icons.entrySet().stream()
                    .min(Comparator.<Map.Entry<Integer, String>>comparingInt(entry ->
                            entry.getKey() >= size ? entry.getKey() : Integer.MAX_VALUE)
                            .thenComparing((left, right) -> Integer.compare(right.getKey(), left.getKey())))
                    .map(Map.Entry::getValue);
        }
        @Override public Map<String, CustomValue> getCustomValues() { return customValues; }
    }

    private record SimpleContact(Map<String, String> values) implements ContactInformation {
        SimpleContact { values = Map.copyOf(values); }
        @Override public Optional<String> get(String key) { return Optional.ofNullable(values.get(key)); }
        @Override public Map<String, String> asMap() { return values; }
    }

    private record SimplePerson(FabricPerson source) implements Person {
        @Override public String getName() { return source.name(); }
        @Override public ContactInformation getContact() { return new SimpleContact(source.contact()); }
    }

    private record BridgeDependency(Kind kind, String modId, List<String> ranges) implements ModDependency {
        BridgeDependency { ranges = List.copyOf(ranges); }
        @Override public Kind getKind() { return kind; }
        @Override public String getModId() { return modId; }
        @Override public boolean matches(Version version) {
            return getVersionRequirements().stream().anyMatch(requirement -> requirement.test(version));
        }
        @Override public Collection<VersionPredicate> getVersionRequirements() {
            try {
                return VersionPredicate.parse(ranges);
            } catch (net.fabricmc.loader.api.VersionParsingException exception) {
                throw new IllegalStateException("Invalid Fabric dependency predicate for " + modId, exception);
            }
        }
        @Override public List<VersionInterval> getVersionIntervals() {
            List<VersionInterval> result = new ArrayList<>();
            for (VersionPredicate requirement : getVersionRequirements()) {
                VersionInterval interval = requirement.getInterval();
                if (interval != null) result = new ArrayList<>(VersionInterval.or(result, interval));
            }
            return List.copyOf(result);
        }
        @Override public boolean equals(Object other) {
            return other instanceof ModDependency dependency
                    && kind == dependency.getKind()
                    && modId.equals(dependency.getModId())
                    && getVersionRequirements().equals(dependency.getVersionRequirements());
        }
        @Override public int hashCode() {
            return (kind.ordinal() * 31 + modId.hashCode()) * 257
                    + getVersionRequirements().hashCode();
        }
        @Override public String toString() {
            return "{" + kind.getKey() + " " + modId + " @ ["
                    + String.join(" || ", ranges) + "]}";
        }
    }
}
