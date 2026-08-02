package dev.loaderbridge.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import dev.loaderbridge.api.BridgeEnvironment;
import dev.loaderbridge.scenario.CompatibilityFailurePhase;
import dev.loaderbridge.scenario.ScenarioAction;
import dev.loaderbridge.scenario.ScenarioExecutionContext;
import dev.loaderbridge.scenario.ScenarioStep;
import dev.loaderbridge.scenario.ScenarioStepStatus;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProbeScenarioPluginTest {
    private static final String TOKEN = "loaderbridge-test-token-with-at-least-32-characters";

    @TempDir
    Path temporaryDirectory;

    @Test
    void authenticatesAndAssertsProbeStateOverLoopback() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/v1/registry/", exchange -> {
            boolean authenticated = ("Bearer " + TOKEN).equals(exchange.getRequestHeaders()
                    .getFirst("Authorization"));
            byte[] body = (authenticated ? "registered" : "unauthorized").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(authenticated ? 200 : 401, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            var context = context("http://127.0.0.1:" + server.getAddress().getPort(), TOKEN);
            var step = new ScenarioStep("stone_registry", ScenarioAction.ASSERT_REGISTRY,
                    Duration.ofSeconds(2), Map.of("subject", "minecraft:stone", "equals", "registered"));

            var result = new ProbeScenarioPlugin().execute(context, step);

            assertThat(result.status()).isEqualTo(ScenarioStepStatus.PASSED);
            assertThat(result.code()).isEqualTo("LB-PROBE-ASSERT-PASS");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void returnsAStableScenarioFailureWhenObservedStateDiffers() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/v1/world/", exchange -> {
            byte[] body = "missing".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            var step = new ScenarioStep("world_state", ScenarioAction.ASSERT_WORLD,
                    Duration.ofSeconds(2), Map.of("subject", "fixture", "equals", "loaded"));

            var result = new ProbeScenarioPlugin().execute(
                    context("http://127.0.0.1:" + server.getAddress().getPort(), TOKEN), step);

            assertThat(result.status()).isEqualTo(ScenarioStepStatus.FAILED);
            assertThat(result.failurePhase()).contains(CompatibilityFailurePhase.SCENARIO);
            assertThat(result.code()).isEqualTo("LB-PROBE-ASSERT-004");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void refusesNonLoopbackProbeEndpointsBeforeConnecting() throws Exception {
        var step = new ScenarioStep("remote_probe", ScenarioAction.ASSERT_NETWORK,
                Duration.ofSeconds(2), Map.of("subject", "channel", "equals", "ready"));

        var result = new ProbeScenarioPlugin().execute(context("http://192.0.2.10:8123", TOKEN), step);

        assertThat(result.status()).isEqualTo(ScenarioStepStatus.FAILED);
        assertThat(result.code()).isEqualTo("LB-PROBE-ASSERT-001");
    }

    private ScenarioExecutionContext context(String uri, String token) {
        return new ScenarioExecutionContext(temporaryDirectory, temporaryDirectory,
                BridgeEnvironment.SERVER, Map.of("probe.uri", uri, "probe.token", token));
    }
}
