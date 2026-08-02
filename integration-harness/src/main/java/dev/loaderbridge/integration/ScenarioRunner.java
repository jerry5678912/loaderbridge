package dev.loaderbridge.integration;

import dev.loaderbridge.scenario.CompatibilityFailurePhase;
import dev.loaderbridge.scenario.CompatibilityScenario;
import dev.loaderbridge.scenario.ScenarioAction;
import dev.loaderbridge.scenario.ScenarioExecutionContext;
import dev.loaderbridge.scenario.ScenarioPlugin;
import dev.loaderbridge.scenario.ScenarioRunResult;
import dev.loaderbridge.scenario.ScenarioStep;
import dev.loaderbridge.scenario.ScenarioStepResult;
import dev.loaderbridge.scenario.ScenarioStepStatus;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ScenarioRunner {
    private final Map<ScenarioAction, ScenarioPlugin> plugins;

    public ScenarioRunner(List<ScenarioPlugin> plugins) {
        Map<ScenarioAction, ScenarioPlugin> indexed = new HashMap<>();
        for (ScenarioPlugin plugin : List.copyOf(plugins)) {
            for (ScenarioAction action : plugin.actions()) {
                if (indexed.put(action, plugin) != null) {
                    throw new IllegalArgumentException("Multiple scenario plugins handle " + action.value());
                }
            }
        }
        this.plugins = Map.copyOf(indexed);
    }

    public ScenarioRunResult run(CompatibilityScenario scenario, ScenarioExecutionContext context,
            ScenarioSession session) {
        List<ScenarioStepResult> results = new ArrayList<>();
        for (ScenarioStep step : scenario.steps()) {
            ScenarioStepResult result = execute(context, session, step);
            results.add(result);
            if (result.status() == ScenarioStepStatus.FAILED) {
                break;
            }
        }
        return new ScenarioRunResult(scenario.id(), results);
    }

    private ScenarioStepResult execute(ScenarioExecutionContext context, ScenarioSession session,
            ScenarioStep step) {
        long started = System.nanoTime();
        try {
            if (!isBuiltIn(step.action())) {
                ScenarioPlugin plugin = plugins.get(step.action());
                if (plugin == null) {
                    throw new StepFailure(CompatibilityFailurePhase.SCENARIO,
                            "LB-SCENARIO-ACTION-001",
                            "No scenario plugin handles action " + step.action().value());
                }
                return java.util.Objects.requireNonNull(plugin.execute(context, step),
                        "Scenario plugin returned no result");
            }
            String message = executeBuiltIn(context, session, step);
            return ScenarioStepResult.passed("LB-SCENARIO-STEP-PASS", message,
                    elapsed(started), session.artifacts());
        } catch (StepFailure exception) {
            return ScenarioStepResult.failure(exception.phase, exception.code, exception.getMessage(),
                    elapsed(started), session.artifacts());
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return ScenarioStepResult.failure(phase(step.action()), "LB-SCENARIO-STEP-001",
                    exception.getClass().getSimpleName() + ": " + safeMessage(exception),
                    elapsed(started), session.artifacts());
        }
    }

    private String executeBuiltIn(ScenarioExecutionContext context, ScenarioSession session,
            ScenarioStep step) throws Exception {
        ScenarioAction action = step.action();
        if (action.equals(ScenarioAction.START_INSTANCE)) {
            session.start(step.timeout());
            return "Instance started";
        }
        if (action.equals(ScenarioAction.WAIT_FOR_LOG)) {
            requireObserved(session.awaitLog(required(step, "contains"), step.timeout()), step,
                    CompatibilityFailurePhase.LIFECYCLE, "LB-SCENARIO-LOG-001");
            return "Log marker observed";
        }
        if (action.equals(ScenarioAction.SEND_COMMAND)) {
            session.sendCommand(required(step, "command"), step.timeout());
            return "Command sent";
        }
        if (action.equals(ScenarioAction.SAVE)) {
            session.sendCommand(step.parameters().getOrDefault("command", "save-all flush"), step.timeout());
            requireObserved(session.awaitLog(required(step, "marker"), step.timeout()), step,
                    CompatibilityFailurePhase.SAVE, "LB-SCENARIO-SAVE-001");
            return "World save marker observed";
        }
        if (action.equals(ScenarioAction.RELOAD)) {
            session.reload(step.timeout());
            requireObserved(session.awaitLog(required(step, "marker"), step.timeout()), step,
                    CompatibilityFailurePhase.RELOAD, "LB-SCENARIO-RELOAD-001");
            return "Instance reloaded";
        }
        if (action.equals(ScenarioAction.SHUTDOWN)) {
            String marker = step.parameters().get("marker");
            requireObserved(session.shutdown(marker, step.timeout()), step,
                    CompatibilityFailurePhase.SAVE, "LB-SCENARIO-SHUTDOWN-001");
            return "Instance stopped cleanly";
        }
        throw new IllegalStateException("Unrecognized built-in action " + action.value());
    }

    private static String required(ScenarioStep step, String name) {
        String value = step.parameters().get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Step " + step.id() + " requires parameter " + name);
        }
        return value;
    }

    private static void requireObserved(boolean observed, ScenarioStep step,
            CompatibilityFailurePhase phase, String code) throws StepFailure {
        if (!observed) {
            throw new StepFailure(phase, code, "Expected marker was not observed for step " + step.id());
        }
    }

    private static CompatibilityFailurePhase phase(ScenarioAction action) {
        if (action.equals(ScenarioAction.START_INSTANCE) || action.equals(ScenarioAction.WAIT_FOR_LOG)) {
            return CompatibilityFailurePhase.LIFECYCLE;
        }
        if (action.equals(ScenarioAction.SAVE) || action.equals(ScenarioAction.SHUTDOWN)) {
            return CompatibilityFailurePhase.SAVE;
        }
        if (action.equals(ScenarioAction.RELOAD)) {
            return CompatibilityFailurePhase.RELOAD;
        }
        return CompatibilityFailurePhase.SCENARIO;
    }

    private static boolean isBuiltIn(ScenarioAction action) {
        return action.equals(ScenarioAction.START_INSTANCE) || action.equals(ScenarioAction.WAIT_FOR_LOG)
                || action.equals(ScenarioAction.SEND_COMMAND) || action.equals(ScenarioAction.SAVE)
                || action.equals(ScenarioAction.RELOAD) || action.equals(ScenarioAction.SHUTDOWN);
    }

    private static Duration elapsed(long started) {
        return Duration.ofNanos(Math.max(0, System.nanoTime() - started));
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "no detail" : message;
    }

    private static final class StepFailure extends Exception {
        private static final long serialVersionUID = 1L;
        private final CompatibilityFailurePhase phase;
        private final String code;

        private StepFailure(CompatibilityFailurePhase phase, String code, String message) {
            super(message);
            this.phase = phase;
            this.code = code;
        }
    }
}
