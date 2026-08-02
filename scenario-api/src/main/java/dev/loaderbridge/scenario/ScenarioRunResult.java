package dev.loaderbridge.scenario;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ScenarioRunResult(String scenarioId, List<ScenarioStepResult> steps) {
    public ScenarioRunResult {
        scenarioId = ScenarioStep.identifier(scenarioId, "scenario ID");
        steps = List.copyOf(Objects.requireNonNull(steps, "steps"));
    }

    public boolean succeeded() {
        return !steps.isEmpty() && steps.stream().allMatch(step -> step.status() == ScenarioStepStatus.PASSED);
    }

    public Optional<CompatibilityFailurePhase> failurePhase() {
        return steps.stream().flatMap(step -> step.failurePhase().stream()).findFirst();
    }
}
