package dev.loaderbridge.scenario.yaml;

import static org.assertj.core.api.Assertions.assertThat;

import dev.loaderbridge.api.BridgeEnvironment;
import dev.loaderbridge.scenario.ScenarioAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ControlledScenarioSuiteTest {
    @Test
    void definesAtLeastTwentyFiveUniqueCrossSideBehavioralScenarios() throws Exception {
        Path directory = Path.of(System.getProperty("loaderbridge.controlledScenarios"));
        var parser = new ScenarioYamlParser();
        var scenarios = new java.util.ArrayList<dev.loaderbridge.scenario.CompatibilityScenario>();
        try (var files = Files.list(directory)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".yaml")).sorted().toList()) {
                scenarios.add(parser.parse(file));
            }
        }

        assertThat(scenarios).hasSizeGreaterThanOrEqualTo(25);
        assertThat(scenarios).extracting(dev.loaderbridge.scenario.CompatibilityScenario::id)
                .doesNotHaveDuplicates();
        assertThat(scenarios).extracting(dev.loaderbridge.scenario.CompatibilityScenario::side)
                .contains(BridgeEnvironment.CLIENT, BridgeEnvironment.SERVER);
        Set<ScenarioAction> actions = scenarios.stream().flatMap(scenario -> scenario.steps().stream())
                .map(dev.loaderbridge.scenario.ScenarioStep::action).collect(Collectors.toSet());
        assertThat(actions).contains(ScenarioAction.START_INSTANCE, ScenarioAction.SEND_COMMAND,
                ScenarioAction.ASSERT_REGISTRY, ScenarioAction.ASSERT_NETWORK,
                ScenarioAction.ASSERT_WORLD, ScenarioAction.ASSERT_SCREEN,
                ScenarioAction.ASSERT_RENDER, ScenarioAction.ASSERT_RESOURCE,
                ScenarioAction.ASSERT_CONFIGURATION, ScenarioAction.SAVE,
                ScenarioAction.RELOAD, ScenarioAction.SHUTDOWN);
    }
}
