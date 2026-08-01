package dev.loaderbridge.fabric.metadata;

import java.util.Objects;

public record FabricEntrypoint(String adapter, String value) {
    public FabricEntrypoint {
        Objects.requireNonNull(adapter, "adapter");
        Objects.requireNonNull(value, "value");
    }
}
