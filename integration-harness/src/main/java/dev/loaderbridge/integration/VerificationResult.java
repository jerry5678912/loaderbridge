package dev.loaderbridge.integration;

public record VerificationResult(boolean succeeded, boolean reachedReady, boolean savedWorld, int exitCode,
        String diagnosticCode, String message) {
    public static VerificationResult success() {
        return new VerificationResult(true, true, true, 0, "LB-VERIFY-000",
                "Forge server reached ready state and stopped cleanly");
    }

    public static VerificationResult failure(boolean reachedReady, boolean savedWorld, int exitCode,
            String diagnosticCode, String message) {
        return new VerificationResult(false, reachedReady, savedWorld, exitCode, diagnosticCode, message);
    }
}
