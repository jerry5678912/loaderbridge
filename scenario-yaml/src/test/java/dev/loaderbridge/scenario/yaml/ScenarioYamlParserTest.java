package dev.loaderbridge.scenario.yaml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.loaderbridge.api.BridgeEnvironment;
import dev.loaderbridge.scenario.ScenarioAction;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScenarioYamlParserTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void parsesVersionedScenarioWithoutConstructingArbitraryTypes() throws Exception {
        Path yaml = write("scenario.yaml", """
                schemaVersion: 1
                id: server.lifecycle
                description: Start, validate, save, and stop a server
                side: server
                mods: [fixture]
                steps:
                  - id: ready
                    action: wait_for_log
                    timeout: PT90S
                    parameters:
                      contains: "Done ("
                  - id: shutdown
                    action: shutdown
                    timeout: PT30S
                """);

        var scenario = new ScenarioYamlParser().parse(yaml);

        assertThat(scenario.side()).isEqualTo(BridgeEnvironment.SERVER);
        assertThat(scenario.steps()).hasSize(2);
        assertThat(scenario.steps().getFirst().action()).isEqualTo(ScenarioAction.WAIT_FOR_LOG);
        assertThat(scenario.steps().getFirst().parameters()).containsEntry("contains", "Done (");
    }

    @Test
    void rejectsUnknownFieldsAndYamlAliases() throws Exception {
        Path unknown = write("unknown.yaml", """
                schemaVersion: 1
                id: invalid.field
                description: Invalid
                side: server
                mods: [fixture]
                unexpected: true
                steps: [{id: stop, action: shutdown, timeout: PT10S}]
                """);
        Path alias = write("alias.yaml", """
                schemaVersion: 1
                id: invalid.alias
                description: Invalid
                side: server
                mods: &mods [fixture]
                steps:
                  - {id: stop, action: shutdown, timeout: PT10S, parameters: {copy: *mods}}
                """);

        assertThatThrownBy(() -> new ScenarioYamlParser().parse(unknown))
                .isInstanceOf(ScenarioFormatException.class).hasMessageContaining("unexpected");
        assertThatThrownBy(() -> new ScenarioYamlParser().parse(alias))
                .isInstanceOf(ScenarioFormatException.class).hasMessageContaining("alias");
    }

    @Test
    void rejectsExplicitJavaObjectTags() throws Exception {
        Path tagged = write("tagged.yaml", """
                schemaVersion: 1
                id: invalid.tag
                description: !!java/object:java.lang.ProcessBuilder [echo, unsafe]
                side: server
                mods: [fixture]
                steps: [{id: stop, action: shutdown, timeout: PT10S}]
                """);

        assertThatThrownBy(() -> new ScenarioYamlParser().parse(tagged))
                .isInstanceOf(ScenarioFormatException.class);
    }

    @Test
    void rejectsDocumentsOverTheSafetyLimit() throws Exception {
        Path oversized = temporaryDirectory.resolve("oversized.yaml");
        Files.writeString(oversized, "x".repeat((1 << 20) + 1));

        assertThatThrownBy(() -> new ScenarioYamlParser().parse(oversized))
                .isInstanceOf(IOException.class).hasMessageContaining("limit");
    }

    private Path write(String name, String contents) throws IOException {
        Path path = temporaryDirectory.resolve(name);
        Files.writeString(path, contents);
        return path;
    }
}
