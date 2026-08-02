package dev.loaderbridge.integration;

import dev.loaderbridge.scenario.CompatibilityFailurePhase;
import dev.loaderbridge.scenario.ScenarioAction;
import dev.loaderbridge.scenario.ScenarioExecutionContext;
import dev.loaderbridge.scenario.ScenarioPlugin;
import dev.loaderbridge.scenario.ScenarioStep;
import dev.loaderbridge.scenario.ScenarioStepResult;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Performs authenticated semantic assertions against the test-only game probe. */
public final class ProbeScenarioPlugin implements ScenarioPlugin {
    private static final int MAXIMUM_RESPONSE_BYTES = 1 << 20;
    private static final Set<ScenarioAction> ACTIONS = Set.of(ScenarioAction.ASSERT_REGISTRY,
            ScenarioAction.ASSERT_NETWORK, ScenarioAction.ASSERT_WORLD, ScenarioAction.ASSERT_SCREEN,
            ScenarioAction.ASSERT_RENDER, ScenarioAction.ASSERT_RESOURCE,
            ScenarioAction.ASSERT_CONFIGURATION);
    private static final Map<ScenarioAction, String> PATHS = Map.of(
            ScenarioAction.ASSERT_REGISTRY, "registry",
            ScenarioAction.ASSERT_NETWORK, "network",
            ScenarioAction.ASSERT_WORLD, "world",
            ScenarioAction.ASSERT_SCREEN, "screen",
            ScenarioAction.ASSERT_RENDER, "render",
            ScenarioAction.ASSERT_RESOURCE, "resource",
            ScenarioAction.ASSERT_CONFIGURATION, "configuration");
    private final HttpClient client;

    public ProbeScenarioPlugin() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER).build());
    }

    ProbeScenarioPlugin(HttpClient client) {
        this.client = java.util.Objects.requireNonNull(client, "client");
    }

    @Override
    public String id() {
        return "loaderbridge-loopback-probe-v1";
    }

    @Override
    public Set<ScenarioAction> actions() {
        return ACTIONS;
    }

    @Override
    public ScenarioStepResult execute(ScenarioExecutionContext context, ScenarioStep step) {
        long started = System.nanoTime();
        try {
            URI base = validatedBase(context.runtimeAttributes().get("probe.uri"));
            String token = requiredRuntime(context, "probe.token");
            if (token.length() < 32 || token.length() > 256 || token.indexOf('\r') >= 0
                    || token.indexOf('\n') >= 0) {
                return failure("LB-PROBE-ASSERT-001", "Probe token is invalid", started);
            }
            String kind = PATHS.get(step.action());
            if (kind == null) {
                return failure("LB-PROBE-ASSERT-001", "Unsupported probe action", started);
            }
            String subject = requiredParameter(step, "subject");
            String expected = requiredParameter(step, "equals");
            String encodedSubject = URLEncoder.encode(subject, StandardCharsets.UTF_8).replace("+", "%20");
            URI endpoint = base.resolve("/v1/" + kind + "/" + encodedSubject);
            HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(step.timeout())
                    .header("Authorization", "Bearer " + token)
                    .header("X-LoaderBridge-Scenario", step.id()).GET().build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                byte[] bytes = body.readNBytes(MAXIMUM_RESPONSE_BYTES + 1);
                if (bytes.length > MAXIMUM_RESPONSE_BYTES) {
                    return failure("LB-PROBE-ASSERT-005", "Probe response exceeds 1 MiB", started);
                }
                String observed = new String(bytes, StandardCharsets.UTF_8);
                if (response.statusCode() != 200) {
                    return failure("LB-PROBE-ASSERT-002",
                            "Probe returned HTTP " + response.statusCode(), started);
                }
                if (!observed.equals(expected)) {
                    return failure("LB-PROBE-ASSERT-004",
                            "Probe state mismatch for " + kind + "/" + subject, started);
                }
            }
            return ScenarioStepResult.passed("LB-PROBE-ASSERT-PASS",
                    "Probe assertion passed for " + kind + "/" + subject, elapsed(started), List.of());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return failure("LB-PROBE-ASSERT-003", "Probe assertion was interrupted", started);
        } catch (IllegalArgumentException exception) {
            return failure("LB-PROBE-ASSERT-001", safeMessage(exception), started);
        } catch (Exception exception) {
            return failure("LB-PROBE-ASSERT-003", exception.getClass().getSimpleName() + ": "
                    + safeMessage(exception), started);
        }
    }

    private static URI validatedBase(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Missing runtime attribute probe.uri");
        }
        URI uri = URI.create(value);
        String host = uri.getHost();
        boolean loopback = "127.0.0.1".equals(host) || "0:0:0:0:0:0:0:1".equals(host)
                || "::1".equals(host);
        if (!"http".equals(uri.getScheme()) || !loopback || uri.getPort() < 1
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                || !(uri.getPath().isEmpty() || uri.getPath().equals("/"))) {
            throw new IllegalArgumentException("Probe URI must be an HTTP loopback origin with an explicit port");
        }
        return uri;
    }

    private static String requiredRuntime(ScenarioExecutionContext context, String name) {
        String value = context.runtimeAttributes().get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing runtime attribute " + name);
        }
        return value;
    }

    private static String requiredParameter(ScenarioStep step, String name) {
        String value = step.parameters().get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Probe step requires parameter " + name);
        }
        return value;
    }

    private static ScenarioStepResult failure(String code, String message, long started) {
        return ScenarioStepResult.failure(CompatibilityFailurePhase.SCENARIO, code, message,
                elapsed(started), List.of());
    }

    private static Duration elapsed(long started) {
        return Duration.ofNanos(Math.max(0, System.nanoTime() - started));
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "no detail" : message;
    }
}
