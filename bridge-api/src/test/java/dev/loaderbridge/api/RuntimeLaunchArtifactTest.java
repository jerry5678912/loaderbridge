package dev.loaderbridge.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuntimeLaunchArtifactTest {
    @Test
    void snapshotsJvmArguments() {
        var arguments = new ArrayList<>(List.of("-javaagent:${artifact}"));

        var artifact = new RuntimeLaunchArtifact("agent", "1", "module",
                ".loaderbridge/agents/agent.jar", arguments);
        arguments.clear();

        assertThat(artifact.jvmArguments()).containsExactly("-javaagent:${artifact}");
        assertThatThrownBy(() -> artifact.jvmArguments().add("-Dunsafe=true"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsBlankFieldsAndArguments() {
        assertThatThrownBy(() -> new RuntimeLaunchArtifact("", "1", "module",
                ".loaderbridge/agents/agent.jar", List.of("-javaagent:${artifact}")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RuntimeLaunchArtifact("agent", "1", "module",
                ".loaderbridge/agents/agent.jar", List.of("")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
