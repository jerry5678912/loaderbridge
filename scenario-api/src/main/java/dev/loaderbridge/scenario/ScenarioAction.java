package dev.loaderbridge.scenario;

import java.util.Objects;

public record ScenarioAction(String value) implements Comparable<ScenarioAction> {
    public static final ScenarioAction START_INSTANCE = new ScenarioAction("start_instance");
    public static final ScenarioAction WAIT_FOR_LOG = new ScenarioAction("wait_for_log");
    public static final ScenarioAction SEND_COMMAND = new ScenarioAction("send_command");
    public static final ScenarioAction ASSERT_REGISTRY = new ScenarioAction("assert_registry");
    public static final ScenarioAction ASSERT_NETWORK = new ScenarioAction("assert_network");
    public static final ScenarioAction ASSERT_WORLD = new ScenarioAction("assert_world");
    public static final ScenarioAction ASSERT_SCREEN = new ScenarioAction("assert_screen");
    public static final ScenarioAction ASSERT_RENDER = new ScenarioAction("assert_render");
    public static final ScenarioAction ASSERT_RESOURCE = new ScenarioAction("assert_resource");
    public static final ScenarioAction ASSERT_CONFIGURATION = new ScenarioAction("assert_configuration");
    public static final ScenarioAction SAVE = new ScenarioAction("save");
    public static final ScenarioAction RELOAD = new ScenarioAction("reload");
    public static final ScenarioAction SHUTDOWN = new ScenarioAction("shutdown");

    public ScenarioAction {
        Objects.requireNonNull(value, "value");
        value = value.strip();
        if (!value.matches("[a-z][a-z0-9._:-]{0,127}")) {
            throw new IllegalArgumentException("Invalid scenario action ID");
        }
    }

    @Override
    public int compareTo(ScenarioAction other) {
        return value.compareTo(other.value);
    }
}
