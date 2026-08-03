package dev.loaderbridge.fabric.remap;

import java.util.Map;
import java.util.Objects;

public record PreparationManifest(
        String formatVersion,
        String adapterVersion,
        String minecraftVersion,
        String forgeVersion,
        String sourceNamespace,
        String targetNamespace,
        String parentModId,
        String parentSubLocation,
        Map<String, String> fulfilledFabricDependencies) {
    public PreparationManifest {
        Objects.requireNonNull(formatVersion, "formatVersion");
        Objects.requireNonNull(adapterVersion, "adapterVersion");
        Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        Objects.requireNonNull(forgeVersion, "forgeVersion");
        Objects.requireNonNull(sourceNamespace, "sourceNamespace");
        Objects.requireNonNull(targetNamespace, "targetNamespace");
        fulfilledFabricDependencies = Map.copyOf(fulfilledFabricDependencies);
    }

    public static PreparationManifest pinned(String minecraftVersion, String forgeVersion) {
        return new PreparationManifest("1", FabricAdapterVersion.CURRENT, minecraftVersion, forgeVersion,
                "intermediary", "official", null, null, Map.of());
    }

    public PreparationManifest nested(String parentId, String subLocation) {
        return new PreparationManifest(formatVersion, adapterVersion, minecraftVersion, forgeVersion,
                sourceNamespace, targetNamespace,
                Objects.requireNonNull(parentId, "parentId"),
                Objects.requireNonNull(subLocation, "subLocation"), fulfilledFabricDependencies);
    }

    public PreparationManifest namespaces(String source, String target) {
        return new PreparationManifest(formatVersion, adapterVersion, minecraftVersion, forgeVersion,
                Objects.requireNonNull(source, "source"), Objects.requireNonNull(target, "target"),
                parentModId, parentSubLocation, fulfilledFabricDependencies);
    }

    public PreparationManifest fulfilledFabricDependencies(Map<String, String> dependencies) {
        return new PreparationManifest(formatVersion, adapterVersion, minecraftVersion, forgeVersion,
                sourceNamespace, targetNamespace, parentModId, parentSubLocation,
                Objects.requireNonNull(dependencies, "dependencies"));
    }
}
