package dev.loaderbridge.fabric.metadata;

import java.util.Objects;

public record FabricMixin(String config, String environment) {
    public FabricMixin {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(environment, "environment");
    }
}
