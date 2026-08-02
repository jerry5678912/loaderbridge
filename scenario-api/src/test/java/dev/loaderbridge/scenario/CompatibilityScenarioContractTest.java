package dev.loaderbridge.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.loaderbridge.api.BridgeEnvironment;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CompatibilityScenarioContractTest {
    @Test
    void definesImmutableVersionedScenarioWithStableSteps() {
        Map<String, String> parameters = new java.util.HashMap<>();
        parameters.put("contains", "Done (");
        ScenarioStep step = new ScenarioStep("server-ready", ScenarioAction.WAIT_FOR_LOG,
                Duration.ofSeconds(90), parameters);
        CompatibilityScenario scenario = new CompatibilityScenario(1, "server.lifecycle",
                "Starts and saves a dedicated server", BridgeEnvironment.SERVER,
                List.of("fixture"), List.of(step));
        parameters.put("contains", "changed");

        assertThat(scenario.steps()).containsExactly(step);
        assertThat(step.parameters()).containsEntry("contains", "Done (");
        assertThatThrownBy(() -> step.parameters().put("bad", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsDuplicateStepIdsAndUnboundedTimeouts() {
        ScenarioStep first = new ScenarioStep("ready", ScenarioAction.WAIT_FOR_LOG,
                Duration.ofSeconds(10), Map.of("contains", "Done"));
        ScenarioStep duplicate = new ScenarioStep("ready", ScenarioAction.SHUTDOWN,
                Duration.ofSeconds(10), Map.of());

        assertThatThrownBy(() -> new CompatibilityScenario(1, "duplicate.steps", "invalid",
                BridgeEnvironment.SERVER, List.of("fixture"), List.of(first, duplicate)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Duplicate");
        assertThatThrownBy(() -> new ScenarioStep("slow", ScenarioAction.WAIT_FOR_LOG,
                Duration.ofHours(1), Map.of("contains", "never")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("timeout");
    }

    @Test
    void classifiesFailuresWithMachineReadableCodes() {
        ScenarioStepResult failure = ScenarioStepResult.failure(CompatibilityFailurePhase.SAVE,
                "LB-SCENARIO-SAVE-001", "World save marker was not observed", Duration.ofSeconds(2),
                List.of());
        ScenarioRunResult result = new ScenarioRunResult("server.lifecycle", List.of(failure));

        assertThat(result.succeeded()).isFalse();
        assertThat(result.failurePhase()).contains(CompatibilityFailurePhase.SAVE);
        assertThatThrownBy(() -> ScenarioStepResult.failure(CompatibilityFailurePhase.SCENARIO,
                "bad code", "invalid", Duration.ZERO, List.of()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("code");
    }
}
