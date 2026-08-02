package dev.loaderbridge.scenario;

import dev.loaderbridge.api.BridgeEnvironment;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

public record ScenarioExecutionContext(Path instance, Path artifacts, BridgeEnvironment side,
        Map<String, String> runtimeAttributes) {
    public ScenarioExecutionContext {
        instance = Objects.requireNonNull(instance, "instance").toAbsolutePath().normalize();
        artifacts = Objects.requireNonNull(artifacts, "artifacts").toAbsolutePath().normalize();
        Objects.requireNonNull(side, "side");
        runtimeAttributes = Map.copyOf(Objects.requireNonNull(runtimeAttributes, "runtimeAttributes"));
    }
}
