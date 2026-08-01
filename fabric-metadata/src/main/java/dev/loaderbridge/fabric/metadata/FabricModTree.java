package dev.loaderbridge.fabric.metadata;

import java.util.List;
import java.util.Objects;

public record FabricModTree(FabricModMetadata root, List<FabricModTree> nested) {
    public FabricModTree {
        Objects.requireNonNull(root, "root");
        nested = List.copyOf(nested);
    }
}
