package dev.loaderbridge.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class LoaderBridgeCliTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void preparesFixtureThroughDynamicallyLoadedAdapter() throws Exception {
        Path mods = Files.createDirectories(temporaryDirectory.resolve("mods"));
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(mods.resolve("fixture.jar")))) {
            jar.putNextEntry(new JarEntry("fabric.mod.json"));
            jar.write("{\"schemaVersion\":1,\"id\":\"fixture\",\"version\":\"1.0.0\"}"
                    .getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
        Path output = temporaryDirectory.resolve("prepared");

        int exitCode = new CommandLine(new LoaderBridgeCli()).execute("prepare",
                "--minecraft", "1.21.1", "--host", "forge", "--forge-version", "recommended",
                "--mods", mods.toString(), "--output", output.toString(), "--side", "client");

        assertThat(exitCode).isZero();
        assertThat(output.resolve("fixture-1.0.0-loaderbridge.jar")).exists();
        assertThat(output.resolve("bridge.lock.json")).exists();
        assertThat(output.resolve("compatibility-report.json")).exists();
    }

    @Test
    void rejectsMissingInputWithStableExitCode() {
        int exitCode = new CommandLine(new LoaderBridgeCli()).execute("inspect",
                temporaryDirectory.resolve("missing.jar").toString(), "--json");

        assertThat(exitCode).isEqualTo(LoaderBridgeCli.INVALID_INPUT);
    }
}
