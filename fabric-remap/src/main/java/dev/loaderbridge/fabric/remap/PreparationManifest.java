package dev.loaderbridge.fabric.remap;

import java.util.Objects;

public record PreparationManifest(
        String formatVersion,
        String adapterVersion,
        String minecraftVersion,
        String forgeVersion,
        String sourceNamespace,
        String targetNamespace,
        String parentModId,
        String parentSubLocation) {
    public PreparationManifest {
        Objects.requireNonNull(formatVersion, "formatVersion");
        Objects.requireNonNull(adapterVersion, "adapterVersion");
        Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        Objects.requireNonNull(forgeVersion, "forgeVersion");
        Objects.requireNonNull(sourceNamespace, "sourceNamespace");
        Objects.requireNonNull(targetNamespace, "targetNamespace");
    }

    public static PreparationManifest pinned(String minecraftVersion, String forgeVersion) {
        return new PreparationManifest("1", "0.1.0", minecraftVersion, forgeVersion,
                "intermediary", "official", null, null);
    }

    public PreparationManifest nested(String parentId, String subLocation) {
        return new PreparationManifest(formatVersion, adapterVersion, minecraftVersion, forgeVersion,
                sourceNamespace, targetNamespace,
                Objects.requireNonNull(parentId, "parentId"),
                Objects.requireNonNull(subLocation, "subLocation"));
    }
}
