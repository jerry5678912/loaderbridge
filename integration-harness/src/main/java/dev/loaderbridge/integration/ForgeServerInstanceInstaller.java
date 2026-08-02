package dev.loaderbridge.integration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** Installs a checksum-pinned Forge installer into a new or empty server directory. */
public final class ForgeServerInstanceInstaller {
    private static final Duration MAXIMUM_TIMEOUT = Duration.ofMinutes(30);

    public ForgeInstallationResult install(Path installer, String expectedSha256, Path instance,
            Duration timeout) {
        Objects.requireNonNull(installer, "installer");
        Objects.requireNonNull(expectedSha256, "expectedSha256");
        Objects.requireNonNull(instance, "instance");
        Objects.requireNonNull(timeout, "timeout");
        Path source = installer.toAbsolutePath().normalize();
        Path target = instance.toAbsolutePath().normalize();
        Path transcript = target.resolve("loaderbridge-forge-install.log");
        if (timeout.isZero() || timeout.isNegative() || timeout.compareTo(MAXIMUM_TIMEOUT) > 0) {
            return failure("LB-LAB-INSTALL-001", "Installer timeout must be between 1 ms and 30 minutes",
                    transcript);
        }
        String expected = expectedSha256.toLowerCase(Locale.ROOT);
        if (!expected.matches("[0-9a-f]{64}")) {
            return failure("LB-LAB-INSTALL-001", "Expected installer SHA-256 must contain 64 hex characters",
                    transcript);
        }
        if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
            return failure("LB-LAB-INSTALL-001", "Forge installer is not a regular file: " + source,
                    transcript);
        }

        try {
            String actual = sha256(source);
            if (!actual.equals(expected)) {
                return failure("LB-LAB-INSTALL-002", "Forge installer SHA-256 does not match the lock",
                        transcript);
            }
            if (!isUnusedDirectory(target)) {
                return failure("LB-LAB-INSTALL-003",
                        "Refusing to install over a non-empty or non-directory path: " + target, transcript);
            }
            Files.createDirectories(target);
            ProcessBuilder builder = new ProcessBuilder(javaCommand(), "-jar", source.toString(),
                    "--installServer", target.toString()).directory(target.toFile()).redirectErrorStream(true)
                    .redirectOutput(transcript.toFile());
            Process process = builder.start();
            boolean exited;
            try {
                exited = process.waitFor(timeout.toNanos(), TimeUnit.NANOSECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                terminate(process);
                return failure("LB-LAB-INSTALL-004", "Forge installation was interrupted", transcript);
            }
            if (!exited) {
                terminate(process);
                return failure("LB-LAB-INSTALL-005", "Forge installer exceeded its timeout", transcript);
            }
            if (process.exitValue() != 0) {
                return failure("LB-LAB-INSTALL-006",
                        "Forge installer exited with code " + process.exitValue(), transcript);
            }
            Path script = target.resolve(windows() ? "run.bat" : "run.sh");
            if (!Files.isRegularFile(script, LinkOption.NOFOLLOW_LINKS)) {
                return failure("LB-LAB-INSTALL-007",
                        "Forge installer exited successfully but did not create " + script.getFileName(), transcript);
            }
            return ForgeInstallationResult.success(transcript);
        } catch (IOException exception) {
            return failure("LB-LAB-INSTALL-008", "Forge installation I/O failed: " + safeMessage(exception),
                    transcript);
        }
    }

    private static boolean isUnusedDirectory(Path target) throws IOException {
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            return true;
        }
        if (!Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        try (var entries = Files.list(target)) {
            return entries.findAny().isEmpty();
        }
    }

    private static String sha256(Path source) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(source)) {
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, count);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by Java", exception);
        }
    }

    private static String javaCommand() {
        return Path.of(System.getProperty("java.home"), "bin", windows() ? "java.exe" : "java").toString();
    }

    private static boolean windows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static void terminate(Process process) {
        process.destroy();
        try {
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    private static ForgeInstallationResult failure(String code, String message, Path transcript) {
        return ForgeInstallationResult.failure(code, message, transcript);
    }

    private static String safeMessage(IOException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? "no detail" : exception.getMessage();
    }
}
