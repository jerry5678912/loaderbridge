package dev.loaderbridge.fabric.metadata;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record FabricModMetadata(
        int schemaVersion,
        String id,
        String version,
        String name,
        String environment,
        Map<String, List<FabricEntrypoint>> entrypoints,
        FabricDependencies dependencies,
        List<String> provides,
        List<FabricMixin> mixins,
        Optional<String> accessWidener,
        List<String> nestedJars,
        Map<String, String> languageAdapters) {
    public FabricModMetadata {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(environment, "environment");
        entrypoints = entrypoints.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
        Objects.requireNonNull(dependencies, "dependencies");
        provides = List.copyOf(provides);
        mixins = List.copyOf(mixins);
        Objects.requireNonNull(accessWidener, "accessWidener");
        nestedJars = List.copyOf(nestedJars);
        languageAdapters = Map.copyOf(languageAdapters);
    }
}
