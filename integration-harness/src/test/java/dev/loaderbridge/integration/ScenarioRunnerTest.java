package dev.loaderbridge.integration;

import static org.assertj.core.api.Assertions.assertThat;

import dev.loaderbridge.api.BridgeEnvironment;
import dev.loaderbridge.scenario.CompatibilityFailurePhase;
import dev.loaderbridge.scenario.CompatibilityScenario;
import dev.loaderbridge.scenario.ScenarioAction;
import dev.loaderbridge.scenario.ScenarioExecutionContext;
import dev.loaderbridge.scenario.ScenarioPlugin;
import dev.loaderbridge.scenario.ScenarioStep;
import dev.loaderbridge.scenario.ScenarioStepResult;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScenarioRunnerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void executesLifecycleCommandSaveReloadAndShutdownSteps() {
        CompatibilityScenario scenario = new CompatibilityScenario(1, "server.deep",
                "Deep server lifecycle", BridgeEnvironment.SERVER, List.of("fixture"), List.of(
                        step("start", ScenarioAction.START_INSTANCE, Map.of()),
                        step("ready", ScenarioAction.WAIT_FOR_LOG, Map.of("contains", "SERVER_READY")),
                        step("command", ScenarioAction.SEND_COMMAND, Map.of("command", "say tested")),
                        step("save", ScenarioAction.SAVE, Map.of("marker", "WORLD_SAVED")),
                        step("reload", ScenarioAction.RELOAD, Map.of("marker", "SERVER_READY")),
                        step("stop", ScenarioAction.SHUTDOWN, Map.of("marker", "WORLD_SAVED"))));
        FakeSession session = new FakeSession();

        var result = new ScenarioRunner(List.of()).run(scenario, context(), session);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.steps()).hasSize(6);
        assertThat(session.commands).containsExactly("say tested", "save-all flush", "stop");
        assertThat(session.reloadCount).isEqualTo(1);
    }

    @Test
    void classifiesSaveFailureAndStopsBeforeLaterSteps() {
        CompatibilityScenario scenario = new CompatibilityScenario(1, "server.save_failure",
                "Save marker must be observed", BridgeEnvironment.SERVER, List.of("fixture"), List.of(
                        step("start", ScenarioAction.START_INSTANCE, Map.of()),
                        step("save", ScenarioAction.SAVE, Map.of("marker", "MISSING")),
                        step("stop", ScenarioAction.SHUTDOWN, Map.of())));
        FakeSession session = new FakeSession();

        var result = new ScenarioRunner(List.of()).run(scenario, context(), session);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.failurePhase()).contains(CompatibilityFailurePhase.SAVE);
        assertThat(result.steps()).hasSize(2);
    }

    @Test
    void preservesDynamicPluginResults() {
        ScenarioAction action = new ScenarioAction("example:assert_state");
        Path evidence = temporaryDirectory.resolve("evidence.json");
        ScenarioPlugin plugin = new ScenarioPlugin() {
            @Override
            public String id() {
                return "example";
            }

            @Override
            public java.util.Set<ScenarioAction> actions() {
                return java.util.Set.of(action);
            }

            @Override
            public ScenarioStepResult execute(ScenarioExecutionContext context, ScenarioStep step) {
                return ScenarioStepResult.passed("LB-EXAMPLE-STATE-001", "Custom state matched",
                        Duration.ofMillis(1), List.of(evidence));
            }
        };
        CompatibilityScenario scenario = new CompatibilityScenario(1, "plugin.result", "Plugin result",
                BridgeEnvironment.SERVER, List.of("fixture"), List.of(step("custom", action, Map.of())));

        var result = new ScenarioRunner(List.of(plugin)).run(scenario, context(), new FakeSession());

        assertThat(result.steps().getFirst().code()).isEqualTo("LB-EXAMPLE-STATE-001");
        assertThat(result.steps().getFirst().artifacts()).containsExactly(evidence);
    }

    private ScenarioExecutionContext context() {
        return new ScenarioExecutionContext(temporaryDirectory.resolve("instance"),
                temporaryDirectory.resolve("artifacts"), BridgeEnvironment.SERVER, Map.of());
    }

    private static ScenarioStep step(String id, ScenarioAction action, Map<String, String> parameters) {
        return new ScenarioStep(id, action, Duration.ofSeconds(5), parameters);
    }

    private static final class FakeSession implements ScenarioSession {
        private final List<String> commands = new ArrayList<>();
        private boolean running;
        private int reloadCount;

        @Override
        public void start(Duration timeout) {
            running = true;
        }

        @Override
        public boolean awaitLog(String marker, Duration timeout) {
            return running && (marker.equals("SERVER_READY") || marker.equals("WORLD_SAVED"));
        }

        @Override
        public void sendCommand(String command, Duration timeout) {
            commands.add(command);
        }

        @Override
        public void reload(Duration timeout) {
            reloadCount++;
            running = true;
        }

        @Override
        public boolean shutdown(String marker, Duration timeout) {
            commands.add("stop");
            running = false;
            return marker == null || marker.equals("WORLD_SAVED");
        }

        @Override
        public List<Path> artifacts() {
            return List.of();
        }
    }
}
