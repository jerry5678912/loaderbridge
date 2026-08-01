package dev.loaderbridge.fabric.metadata;

import java.util.List;
import java.util.Map;

public record FabricDependencies(
        Map<String, List<String>> depends,
        Map<String, List<String>> recommends,
        Map<String, List<String>> suggests,
        Map<String, List<String>> breaks,
        Map<String, List<String>> conflicts) {
    public FabricDependencies {
        depends = immutableNestedMap(depends);
        recommends = immutableNestedMap(recommends);
        suggests = immutableNestedMap(suggests);
        breaks = immutableNestedMap(breaks);
        conflicts = immutableNestedMap(conflicts);
    }

    private static Map<String, List<String>> immutableNestedMap(Map<String, List<String>> source) {
        return source.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    }
}
