package dev.loaderbridge.api;

import java.nio.file.Path;
import java.util.Objects;

public record Diagnostic(
        DiagnosticSeverity severity,
        String code,
        BridgePhase phase,
        String modId,
        Path artifact,
        String message,
        String causeSummary) {
    public Diagnostic {
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(message, "message");
        if (!code.matches("LB-[A-Z]+-[0-9]{3}")) {
            throw new IllegalArgumentException("Diagnostic code must be stable: " + code);
        }
    }
}
