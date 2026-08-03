package dev.loaderbridge.scenario.yaml;

import static org.assertj.core.api.Assertions.assertThat;

import dev.loaderbridge.scenario.ScenarioAction;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RealModScenarioSuiteTest {
    @Test
    void everyRealModScenarioParsesAndContentProbesAssertBehavior() throws Exception {
        Path directory = Path.of(System.getProperty("loaderbridge.realModScenarios"));
        var parser = new ScenarioYamlParser();
        var scenarios = new java.util.ArrayList<dev.loaderbridge.scenario.CompatibilityScenario>();
        try (var files = Files.list(directory)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".yaml"))
                    .sorted().toList()) {
                scenarios.add(parser.parse(file));
            }
        }

        assertThat(scenarios).isNotEmpty();
        assertThat(scenarios).extracting(dev.loaderbridge.scenario.CompatibilityScenario::id)
                .doesNotHaveDuplicates();
        var blockus = scenarios.stream().filter(scenario -> scenario.id().equals(
                "blockus_content_save_reload")).findFirst().orElseThrow();
        assertThat(blockus.steps()).extracting(dev.loaderbridge.scenario.ScenarioStep::action)
                .contains(ScenarioAction.ASSERT_REGISTRY, ScenarioAction.ASSERT_WORLD,
                        ScenarioAction.ASSERT_RENDER, ScenarioAction.SAVE,
                        ScenarioAction.RELOAD, ScenarioAction.SHUTDOWN);
    }
}
