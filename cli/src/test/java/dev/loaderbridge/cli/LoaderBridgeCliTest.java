package dev.loaderbridge.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class LoaderBridgeCliTest {

    @Test
    void rejectsInvalidCatalogFreezeInputsBeforeRepositoryAccess() {
        int exitCode = new CommandLine(new LoaderBridgeCli()).execute("catalog", "freeze",
                "--snapshot-id", "2026-08", "--frozen-at", "not-a-time", "--output", "snapshot.json");

        assertThat(exitCode).isEqualTo(LoaderBridgeCli.INVALID_INPUT);
    }

    @Test
    void rejectsUnqualifiedRepositoryProjectIds() {
        int exitCode = new CommandLine(new LoaderBridgeCli()).execute("resolve",
                "--project", "missing-prefix", "--output", "resolved");

        assertThat(exitCode).isEqualTo(LoaderBridgeCli.INVALID_INPUT);
    }
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

    @Test
    void rejectsNonPositiveVerificationTimeout() throws Exception {
        Path instance = Files.createDirectories(temporaryDirectory.resolve("instance"));

        int exitCode = new CommandLine(new LoaderBridgeCli()).execute("verify",
                "--instance", instance.toString(), "--side", "server", "--timeout-seconds", "0");

        assertThat(exitCode).isEqualTo(LoaderBridgeCli.INVALID_INPUT);
    }

    @Test
    void runsADeepServerScenarioAndWritesMachineReadableResults() throws Exception {
        Path instance = Files.createDirectories(temporaryDirectory.resolve("scenario-instance"));
        writeScenarioLaunchScript(instance);
        Path scenario = temporaryDirectory.resolve("server-scenario.yaml");
        Files.writeString(scenario, """
                schemaVersion: 1
                id: fixture_server_cycle
                description: Exercises server readiness, save, reload, and shutdown.
                side: server
                mods:
                  - fixture
                steps:
                  - id: start
                    action: start_instance
                    timeout: PT5S
                  - id: ready
                    action: wait_for_log
                    timeout: PT5S
                    parameters:
                      contains: FIXTURE_READY
                  - id: save
                    action: save
                    timeout: PT5S
                    parameters:
                      marker: WORLD_SAVED
                  - id: reload
                    action: reload
                    timeout: PT5S
                    parameters:
                      marker: FIXTURE_READY
                  - id: stop
                    action: shutdown
                    timeout: PT5S
                    parameters:
                      marker: CLEAN_STOP
                """, StandardCharsets.UTF_8);
        Path artifacts = temporaryDirectory.resolve("scenario-artifacts");

        int exitCode = new CommandLine(new LoaderBridgeCli()).execute("test",
                "--scenario", scenario.toString(), "--instance", instance.toString(),
                "--artifacts", artifacts.toString(), "--json");

        assertThat(exitCode).isZero();
        assertThat(artifacts.resolve("scenario-report.json")).content(StandardCharsets.UTF_8)
                .contains("\"succeeded\": true", "LB-SCENARIO-STEP-PASS");
    }

    @Test
    void runsClientScenariosThroughTheFixedClientLauncher() throws Exception {
        Path instance = Files.createDirectories(temporaryDirectory.resolve("client-instance"));
        writeClientLaunchScript(instance);
        Path scenario = temporaryDirectory.resolve("client-scenario.yaml");
        Files.writeString(scenario, """
                schemaVersion: 1
                id: fixture_client_cycle
                description: Exercises client title readiness and clean shutdown.
                side: client
                mods: [fixture]
                steps:
                  - id: start
                    action: start_instance
                    timeout: PT5S
                  - id: title
                    action: wait_for_log
                    timeout: PT5S
                    parameters:
                      contains: CLIENT_TITLE_READY
                  - id: stop
                    action: shutdown
                    timeout: PT5S
                    parameters:
                      marker: CLIENT_STOPPED
                """, StandardCharsets.UTF_8);

        int exitCode = new CommandLine(new LoaderBridgeCli()).execute("test",
                "--scenario", scenario.toString(), "--instance", instance.toString(),
                "--artifacts", temporaryDirectory.resolve("client-artifacts").toString());

        assertThat(exitCode).isZero();
    }

    @Test
    void loadsTheAuthenticatedProbePluginForSemanticAssertions() throws Exception {
        String token = "loaderbridge-cli-probe-token-with-at-least-32-characters";
        HttpServer probe = HttpServer.create(
                new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        probe.createContext("/v1/registry/", exchange -> {
            boolean authenticated = ("Bearer " + token).equals(
                    exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = (authenticated ? "registered" : "unauthorized")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(authenticated ? 200 : 401, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        probe.start();
        try {
            Path instance = Files.createDirectories(temporaryDirectory.resolve("probe-instance"));
            writeScenarioLaunchScript(instance);
            Path tokenFile = temporaryDirectory.resolve("probe.token");
            Files.writeString(tokenFile, token, StandardCharsets.UTF_8);
            Path scenario = temporaryDirectory.resolve("probe-scenario.yaml");
            Files.writeString(scenario, """
                    schemaVersion: 1
                    id: fixture_probe_cycle
                    description: Uses the authenticated semantic probe.
                    side: server
                    mods: [fixture]
                    steps:
                      - id: start
                        action: start_instance
                        timeout: PT5S
                      - id: stone
                        action: assert_registry
                        timeout: PT5S
                        parameters:
                          subject: minecraft:stone
                          equals: registered
                      - id: stop
                        action: shutdown
                        timeout: PT5S
                        parameters:
                          marker: CLEAN_STOP
                    """, StandardCharsets.UTF_8);

            int exitCode = new CommandLine(new LoaderBridgeCli()).execute("test",
                    "--scenario", scenario.toString(), "--instance", instance.toString(),
                    "--artifacts", temporaryDirectory.resolve("probe-artifacts").toString(),
                    "--probe-uri", "http://127.0.0.1:" + probe.getAddress().getPort(),
                    "--probe-token-file", tokenFile.toString());

            assertThat(exitCode).isZero();
        } finally {
            probe.stop(0);
        }
    }

    private static void writeScenarioLaunchScript(Path instance) throws Exception {
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        Path script = instance.resolve(windows ? "run.bat" : "run.sh");
        String contents = windows
                ? "@echo off\r\necho FIXTURE_READY\r\n:loop\r\nset /p command=\r\n"
                        + "if \"%command%\"==\"save-all flush\" echo WORLD_SAVED\r\n"
                        + "if \"%command%\"==\"stop\" echo CLEAN_STOP& exit /b 0\r\ngoto loop\r\n"
                : "#!/usr/bin/env sh\nprintf '%s\\n' FIXTURE_READY\nwhile read command; do\n"
                        + "  if [ \"$command\" = 'save-all flush' ]; then printf '%s\\n' WORLD_SAVED; fi\n"
                        + "  if [ \"$command\" = stop ]; then printf '%s\\n' CLEAN_STOP; exit 0; fi\n"
                        + "done\n";
        Files.writeString(script, contents, StandardCharsets.UTF_8);
    }

    private static void writeClientLaunchScript(Path instance) throws Exception {
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        Path script = instance.resolve(windows ? "run-client.bat" : "run-client.sh");
        String contents = windows
                ? "@echo off\r\necho CLIENT_TITLE_READY\r\nset /p command=\r\necho CLIENT_STOPPED\r\n"
                : "#!/usr/bin/env sh\nprintf '%s\\n' CLIENT_TITLE_READY\nread command\n"
                        + "printf '%s\\n' CLIENT_STOPPED\n";
        Files.writeString(script, contents, StandardCharsets.UTF_8);
    }
}
