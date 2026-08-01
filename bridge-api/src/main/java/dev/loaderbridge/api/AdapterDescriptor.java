package dev.loaderbridge.api;

import java.util.List;
import java.util.Objects;

public record AdapterDescriptor(
        String id,
        String contractVersion,
        LoaderId sourceLoader,
        LoaderId targetLoader,
        String minecraftVersionRange,
        String hostVersionRange,
        List<BridgeCapability> capabilities) {
    public AdapterDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(contractVersion, "contractVersion");
        Objects.requireNonNull(sourceLoader, "sourceLoader");
        Objects.requireNonNull(targetLoader, "targetLoader");
        Objects.requireNonNull(minecraftVersionRange, "minecraftVersionRange");
        Objects.requireNonNull(hostVersionRange, "hostVersionRange");
        capabilities = List.copyOf(capabilities);
    }
}
