package dev.loaderbridge.scenario;

import dev.loaderbridge.api.BridgeEnvironment;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record CompatibilityScenario(int schemaVersion, String id, String description,
        BridgeEnvironment side, List<String> modIds, List<ScenarioStep> steps) {
    public CompatibilityScenario {
        if (schemaVersion != 1) {
            throw new IllegalArgumentException("Unsupported scenario schema version");
        }
        id = ScenarioStep.identifier(id, "scenario id");
        Objects.requireNonNull(description, "description");
        description = description.strip();
        if (description.isEmpty() || description.length() > 2048) {
            throw new IllegalArgumentException("Invalid scenario description");
        }
        Objects.requireNonNull(side, "side");
        modIds = List.copyOf(Objects.requireNonNull(modIds, "modIds"));
        if (modIds.isEmpty() || modIds.size() > 256) {
            throw new IllegalArgumentException("A scenario requires 1 to 256 mod IDs");
        }
        modIds = modIds.stream().map(value -> ScenarioStep.identifier(value, "mod ID")).toList();
        steps = List.copyOf(Objects.requireNonNull(steps, "steps"));
        if (steps.isEmpty() || steps.size() > 256) {
            throw new IllegalArgumentException("A scenario requires 1 to 256 steps");
        }
        HashSet<String> stepIds = new HashSet<>();
        Duration total = Duration.ZERO;
        for (ScenarioStep step : steps) {
            if (!stepIds.add(step.id())) {
                throw new IllegalArgumentException("Duplicate scenario step ID: " + step.id());
            }
            total = total.plus(step.timeout());
        }
        if (total.compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalArgumentException("Scenario timeout budget exceeds one hour");
        }
    }
}
