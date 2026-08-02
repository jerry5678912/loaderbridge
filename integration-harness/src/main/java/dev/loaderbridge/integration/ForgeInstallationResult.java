package dev.loaderbridge.integration;

import java.nio.file.Path;
import java.util.Objects;

public record ForgeInstallationResult(boolean succeeded, String code, String message, Path transcript) {
    public ForgeInstallationResult {
        if (succeeded != code.equals("LB-LAB-INSTALL-PASS")) {
            throw new IllegalArgumentException("Success state and installation code disagree");
        }
        if (!code.matches("LB-LAB-INSTALL-(?:PASS|[0-9]{3})")) {
            throw new IllegalArgumentException("Invalid installation diagnostic code");
        }
        message = Objects.requireNonNull(message, "message").strip();
        if (message.isEmpty() || message.length() > 4096) {
            throw new IllegalArgumentException("Invalid installation result message");
        }
        transcript = Objects.requireNonNull(transcript, "transcript").toAbsolutePath().normalize();
    }

    public static ForgeInstallationResult success(Path transcript) {
        return new ForgeInstallationResult(true, "LB-LAB-INSTALL-PASS",
                "Forge server installed into a disposable instance", transcript);
    }

    public static ForgeInstallationResult failure(String code, String message, Path transcript) {
        return new ForgeInstallationResult(false, code, message, transcript);
    }
}
