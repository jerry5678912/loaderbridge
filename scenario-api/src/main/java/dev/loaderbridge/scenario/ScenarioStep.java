package dev.loaderbridge.scenario;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record ScenarioStep(String id, ScenarioAction action, Duration timeout,
        Map<String, String> parameters) {
    private static final Duration MAXIMUM_TIMEOUT = Duration.ofMinutes(10);

    public ScenarioStep {
        id = identifier(id, "step id");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative() || timeout.compareTo(MAXIMUM_TIMEOUT) > 0) {
            throw new IllegalArgumentException("Step timeout must be between 1 ms and 10 minutes");
        }
        Objects.requireNonNull(parameters, "parameters");
        if (parameters.size() > 64) {
            throw new IllegalArgumentException("A scenario step can have at most 64 parameters");
        }
        LinkedHashMap<String, String> validated = new LinkedHashMap<>();
        parameters.forEach((key, value) -> {
            String normalizedKey = identifier(key, "parameter key");
            Objects.requireNonNull(value, "parameter value");
            if (value.length() > 4096) {
                throw new IllegalArgumentException("Scenario parameter value is too long");
            }
            validated.put(normalizedKey, value);
        });
        parameters = Map.copyOf(validated);
    }

    static String identifier(String value, String label) {
        Objects.requireNonNull(value, label);
        String normalized = value.strip();
        if (!normalized.matches("[a-z][a-z0-9._-]{0,127}")) {
            throw new IllegalArgumentException("Invalid " + label);
        }
        return normalized;
    }
}
