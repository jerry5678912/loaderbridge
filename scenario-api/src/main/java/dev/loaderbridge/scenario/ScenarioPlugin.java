package dev.loaderbridge.scenario;

import java.util.Set;

/** ServiceLoader extension for scenario actions that cannot be expressed by built-in steps. */
public interface ScenarioPlugin {
    String id();

    Set<ScenarioAction> actions();

    ScenarioStepResult execute(ScenarioExecutionContext context, ScenarioStep step) throws Exception;
}
