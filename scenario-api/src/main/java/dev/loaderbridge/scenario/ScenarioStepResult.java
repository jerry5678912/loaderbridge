package dev.loaderbridge.scenario;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ScenarioStepResult(ScenarioStepStatus status,
        Optional<CompatibilityFailurePhase> failurePhase, String code, String message,
        Duration elapsed, List<Path> artifacts) {
    public ScenarioStepResult {
        Objects.requireNonNull(status, "status");
        failurePhase = Objects.requireNonNull(failurePhase, "failurePhase");
        if (status == ScenarioStepStatus.FAILED && failurePhase.isEmpty()
                || status != ScenarioStepStatus.FAILED && failurePhase.isPresent()) {
            throw new IllegalArgumentException("Failure phase must match failed status");
        }
        Objects.requireNonNull(code, "code");
        if (!code.matches("LB-[A-Z0-9-]{3,96}")) {
            throw new IllegalArgumentException("Invalid diagnostic code");
        }
        Objects.requireNonNull(message, "message");
        message = message.strip();
        if (message.isEmpty() || message.length() > 4096) {
            throw new IllegalArgumentException("Invalid result message");
        }
        Objects.requireNonNull(elapsed, "elapsed");
        if (elapsed.isNegative()) {
            throw new IllegalArgumentException("Elapsed duration cannot be negative");
        }
        artifacts = List.copyOf(Objects.requireNonNull(artifacts, "artifacts"));
    }

    public static ScenarioStepResult passed(String code, String message, Duration elapsed,
            List<Path> artifacts) {
        return new ScenarioStepResult(ScenarioStepStatus.PASSED, Optional.empty(), code, message,
                elapsed, artifacts);
    }

    public static ScenarioStepResult failure(CompatibilityFailurePhase phase, String code,
            String message, Duration elapsed, List<Path> artifacts) {
        return new ScenarioStepResult(ScenarioStepStatus.FAILED, Optional.of(phase), code, message,
                elapsed, artifacts);
    }
}
