package dev.loaderbridge.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.loaderbridge.api.BridgeEnvironment;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ForgeProcessScenarioSessionTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void drivesCommandsCleanShutdownAndReloadAcrossProcessGenerations() throws Exception {
        Path instance = Files.createDirectories(temporaryDirectory.resolve("instance"));
        Path artifacts = temporaryDirectory.resolve("artifacts");
        try (var session = new ForgeProcessScenarioSession(instance, artifacts, ignored -> fixtureCommand())) {
            session.start(Duration.ofSeconds(5));
            assertThat(session.awaitLog("FIXTURE_READY", Duration.ofSeconds(5))).isTrue();

            session.sendCommand("save-all flush", Duration.ofSeconds(1));
            assertThat(session.awaitLog("WORLD_SAVED", Duration.ofSeconds(5))).isTrue();

            session.reload(Duration.ofSeconds(5));
            assertThat(session.awaitLog("FIXTURE_READY generation=2", Duration.ofSeconds(5))).isTrue();
            assertThat(session.shutdown("CLEAN_STOP", Duration.ofSeconds(5))).isTrue();

            assertThat(session.artifacts()).hasSize(2).allMatch(Files::isRegularFile);
            assertThat(Files.readString(session.artifacts().get(1), StandardCharsets.UTF_8))
                    .contains("FIXTURE_READY generation=2", "CLEAN_STOP");
        }
    }

    @Test
    void rejectsMultilineConsoleCommands() throws Exception {
        Path instance = Files.createDirectories(temporaryDirectory.resolve("instance"));
        try (var session = new ForgeProcessScenarioSession(instance, temporaryDirectory.resolve("artifacts"),
                ignored -> fixtureCommand())) {
            session.start(Duration.ofSeconds(5));

            assertThatThrownBy(() -> session.sendCommand("stop\nop me", Duration.ofSeconds(1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("single line");
        }
    }

    @Test
    void discoversForgeLogsAndCrashReports() throws Exception {
        Path instance = Files.createDirectories(temporaryDirectory.resolve("instance"));
        Path latestLog = Files.createDirectories(instance.resolve("logs")).resolve("latest.log");
        Path crashReport = Files.createDirectories(instance.resolve("crash-reports")).resolve("crash.txt");
        Files.writeString(latestLog, "latest", StandardCharsets.UTF_8);
        Files.writeString(crashReport, "crash", StandardCharsets.UTF_8);

        try (var session = new ForgeProcessScenarioSession(instance, temporaryDirectory.resolve("artifacts"),
                ignored -> fixtureCommand())) {
            assertThat(session.artifacts()).containsExactly(latestLog, crashReport);
        }
    }

    @Test
    void launchesTheFixedClientScriptForClientScenarios() throws Exception {
        Path instance = Files.createDirectories(temporaryDirectory.resolve("client-instance"));
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        Path script = instance.resolve(windows ? "run-client.bat" : "run-client.sh");
        String contents = windows
                ? "@echo off\r\necho CLIENT_TITLE_READY\r\nset /p command=\r\necho CLIENT_STOPPED\r\n"
                : "#!/usr/bin/env sh\nprintf '%s\\n' CLIENT_TITLE_READY\nread command\n"
                        + "printf '%s\\n' CLIENT_STOPPED\n";
        Files.writeString(script, contents, StandardCharsets.UTF_8);

        try (var session = new ForgeProcessScenarioSession(instance,
                temporaryDirectory.resolve("client-artifacts"), BridgeEnvironment.CLIENT)) {
            session.start(Duration.ofSeconds(5));

            assertThat(session.awaitLog("CLIENT_TITLE_READY", Duration.ofSeconds(5))).isTrue();
            assertThat(session.shutdown("CLIENT_STOPPED", Duration.ofSeconds(5))).isTrue();
        }
    }

    private List<String> fixtureCommand() {
        return List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", System.getProperty("java.class.path"), FakeForgeProcess.class.getName(),
                temporaryDirectory.resolve("generation.txt").toString());
    }

    public static final class FakeForgeProcess {
        private FakeForgeProcess() {
        }

        public static void main(String[] args) throws Exception {
            Path generationFile = Path.of(args[0]);
            int generation = Files.exists(generationFile)
                    ? Integer.parseInt(Files.readString(generationFile, StandardCharsets.UTF_8)) + 1
                    : 1;
            Files.writeString(generationFile, Integer.toString(generation), StandardCharsets.UTF_8);
            System.out.println("FIXTURE_READY generation=" + generation);
            System.out.flush();
            try (var input = new java.io.BufferedReader(
                    new java.io.InputStreamReader(System.in, StandardCharsets.UTF_8))) {
                String command;
                while ((command = input.readLine()) != null) {
                    if (command.equals("save-all flush")) {
                        System.out.println("WORLD_SAVED");
                        System.out.flush();
                    } else if (command.equals("stop")) {
                        System.out.println("CLEAN_STOP");
                        System.out.flush();
                        return;
                    }
                }
            }
        }
    }
}
