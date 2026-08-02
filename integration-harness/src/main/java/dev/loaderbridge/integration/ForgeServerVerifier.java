package dev.loaderbridge.integration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class ForgeServerVerifier {
    private static final Object END_OF_OUTPUT = new Object();

    public VerificationResult verify(Path instance, Duration timeout, Consumer<String> output) throws IOException {
        return verify(instance, timeout, output, List.of());
    }

    public VerificationResult verify(Path instance, Duration timeout, Consumer<String> output,
            List<String> expectedMarkers) throws IOException {
        Objects.requireNonNull(instance, "instance");
        Objects.requireNonNull(timeout, "timeout");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(expectedMarkers, "expectedMarkers");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }

        ProcessBuilder builder = new ProcessBuilder(launchCommand(instance)).directory(instance.toFile())
                .redirectErrorStream(true);
        prependCurrentJava(builder);
        Process process = builder.start();
        BlockingQueue<Object> lines = new LinkedBlockingQueue<>();
        Thread reader = Thread.ofVirtual().name("loaderbridge-forge-output").start(() -> readOutput(process, lines));

        boolean reachedReady = false;
        boolean savedWorld = false;
        boolean stopSent = false;
        var missingMarkers = new LinkedHashSet<>(expectedMarkers);
        long deadline = System.nanoTime() + timeout.toNanos();
        try (PrintWriter input = new PrintWriter(process.outputWriter(StandardCharsets.UTF_8), true)) {
            while (System.nanoTime() < deadline) {
                long remaining = Math.max(1, deadline - System.nanoTime());
                Object event = lines.poll(remaining, TimeUnit.NANOSECONDS);
                if (event == null || event == END_OF_OUTPUT) {
                    break;
                }
                String line = (String) event;
                output.accept(line);
                missingMarkers.removeIf(line::contains);
                if (line.contains("Done (") && line.contains("For help")) {
                    reachedReady = true;
                    if (!stopSent) {
                        input.println("stop");
                        stopSent = true;
                    }
                }
                if (line.contains("All dimensions are saved")) {
                    savedWorld = true;
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            terminate(process);
            return VerificationResult.failure(reachedReady, savedWorld, -1, "LB-VERIFY-004",
                    "Verification was interrupted");
        }

        if (process.isAlive()) {
            terminate(process);
            return VerificationResult.failure(reachedReady, savedWorld, -1, "LB-VERIFY-003",
                    "Forge server did not finish within " + timeout.toSeconds() + " seconds");
        }
        try {
            reader.join(Duration.ofSeconds(1));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        int exitCode = process.exitValue();
        if (!reachedReady) {
            return VerificationResult.failure(false, savedWorld, exitCode, "LB-VERIFY-005",
                    "Forge exited before reaching the server-ready marker");
        }
        if (!savedWorld || exitCode != 0) {
            return VerificationResult.failure(true, savedWorld, exitCode, "LB-VERIFY-006",
                    "Forge reached ready state but did not stop and save cleanly");
        }
        if (!missingMarkers.isEmpty()) {
            return VerificationResult.failure(true, true, exitCode, "LB-VERIFY-008",
                    "Forge stopped cleanly but expected markers were missing: " + missingMarkers);
        }
        return VerificationResult.success();
    }

    private static List<String> launchCommand(Path instance) throws IOException {
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        Path script = instance.resolve(windows ? "run.bat" : "run.sh");
        if (!Files.isRegularFile(script)) {
            throw new IOException("Missing Forge launch script: " + script);
        }
        return windows ? List.of("cmd.exe", "/c", script.toString(), "nogui")
                : List.of("sh", script.toString(), "nogui");
    }

    private static void prependCurrentJava(ProcessBuilder builder) {
        String javaBin = Path.of(System.getProperty("java.home"), "bin").toString();
        String oldPath = builder.environment().getOrDefault("PATH", "");
        builder.environment().put("PATH", javaBin + java.io.File.pathSeparator + oldPath);
        builder.environment().put("JAVA_HOME", System.getProperty("java.home"));
    }

    private static void readOutput(Process process, BlockingQueue<Object> lines) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.put(line);
            }
        } catch (IOException exception) {
            lines.offer("LB-VERIFY-007: could not read Forge output: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            lines.offer(END_OF_OUTPUT);
        }
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
}
