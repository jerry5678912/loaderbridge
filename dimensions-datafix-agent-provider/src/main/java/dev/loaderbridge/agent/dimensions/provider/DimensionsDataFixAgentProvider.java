package dev.loaderbridge.agent.dimensions.provider;

import dev.loaderbridge.api.RuntimeLaunchArtifact;
import dev.loaderbridge.api.RuntimeLaunchArtifactProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class DimensionsDataFixAgentProvider implements RuntimeLaunchArtifactProvider {
    private static final RuntimeLaunchArtifact DESCRIPTOR = new RuntimeLaunchArtifact(
            "dimensions-datafix-agent", "1.0.0", "fabric-dimensions-v1-bridge",
            ".loaderbridge/agents/dimensions-datafix-agent.jar",
            List.of("-javaagent:${artifact}"));

    @Override public RuntimeLaunchArtifact descriptor() { return DESCRIPTOR; }

    @Override public Path artifact() throws IOException {
        try {
            Class<?> agent = Class.forName(
                    "dev.loaderbridge.agent.dimensions.DimensionsDataFixAgent", false,
                    getClass().getClassLoader());
            Path path = Path.of(agent.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!Files.isRegularFile(path) || !path.getFileName().toString().endsWith(".jar")) {
                throw new IOException("LB-LAUNCH-002: startup agent is not running from a JAR: "
                        + path);
            }
            return path;
        } catch (ClassNotFoundException exception) {
            throw new IOException("LB-LAUNCH-002: Dimensions startup agent is unavailable",
                    exception);
        } catch (URISyntaxException exception) {
            throw new IOException("LB-LAUNCH-002: invalid startup-agent location", exception);
        }
    }
}
