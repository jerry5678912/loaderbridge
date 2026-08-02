package dev.loaderbridge.integration;

import dev.loaderbridge.api.BridgeEnvironment;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** A stateful scenario session backed by Forge's generated launch script. */
public final class ForgeProcessScenarioSession implements ScenarioSession {
    private static final int MAX_COMMAND_LENGTH = 4096;

    private final Path instance;
    private final Path artifactDirectory;
    private final CommandFactory commandFactory;
    private final Object outputMonitor = new Object();
    private final List<String> currentOutput = new ArrayList<>();
    private final List<Path> sessionLogs = new ArrayList<>();
    private Process process;
    private PrintWriter processInput;
    private Thread outputReader;
    private boolean outputEnded;
    private int launchNumber;

    public ForgeProcessScenarioSession(Path instance, Path artifactDirectory) {
        this(instance, artifactDirectory, BridgeEnvironment.SERVER);
    }

    public ForgeProcessScenarioSession(Path instance, Path artifactDirectory, BridgeEnvironment side) {
        this(instance, artifactDirectory, path -> forgeLaunchCommand(path,
                Objects.requireNonNull(side, "side")));
    }

    ForgeProcessScenarioSession(Path instance, Path artifactDirectory, CommandFactory commandFactory) {
        this.instance = Objects.requireNonNull(instance, "instance").toAbsolutePath().normalize();
        this.artifactDirectory = Objects.requireNonNull(artifactDirectory, "artifactDirectory")
                .toAbsolutePath().normalize();
        this.commandFactory = Objects.requireNonNull(commandFactory, "commandFactory");
    }

    @Override
    public synchronized void start(Duration timeout) throws IOException {
        requirePositive(timeout);
        if (!Files.isDirectory(instance, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Forge instance directory does not exist: " + instance);
        }
        if (process != null && process.isAlive()) {
            throw new IllegalStateException("Forge instance is already running");
        }

        Files.createDirectories(artifactDirectory);
        Path sessionLog;
        do {
            launchNumber++;
            sessionLog = artifactDirectory.resolve("forge-session-%03d.log".formatted(launchNumber));
        } while (Files.exists(sessionLog, LinkOption.NOFOLLOW_LINKS));
        Files.createFile(sessionLog);
        Path launchLog = sessionLog;
        ProcessBuilder builder = new ProcessBuilder(commandFactory.create(instance))
                .directory(instance.toFile()).redirectErrorStream(true);
        prependCurrentJava(builder);
        Process launched = builder.start();
        process = launched;
        processInput = new PrintWriter(launched.outputWriter(StandardCharsets.UTF_8), true);
        synchronized (outputMonitor) {
            currentOutput.clear();
            outputEnded = false;
            sessionLogs.add(launchLog);
        }
        int generation = launchNumber;
        outputReader = Thread.ofVirtual().name("loaderbridge-forge-session-" + generation)
                .start(() -> readOutput(launched, launchLog, generation));
    }

    @Override
    public boolean awaitLog(String marker, Duration timeout) throws InterruptedException {
        Objects.requireNonNull(marker, "marker");
        requirePositive(timeout);
        if (marker.isBlank()) {
            throw new IllegalArgumentException("marker must not be blank");
        }
        long deadline = System.nanoTime() + timeout.toNanos();
        synchronized (outputMonitor) {
            while (true) {
                if (currentOutput.stream().anyMatch(line -> line.contains(marker))) {
                    return true;
                }
                if (outputEnded) {
                    return false;
                }
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    return false;
                }
                TimeUnit.NANOSECONDS.timedWait(outputMonitor, remaining);
            }
        }
    }

    @Override
    public synchronized void sendCommand(String command, Duration timeout) {
        requirePositive(timeout);
        Objects.requireNonNull(command, "command");
        if (command.isBlank() || command.length() > MAX_COMMAND_LENGTH) {
            throw new IllegalArgumentException("command must contain 1 to " + MAX_COMMAND_LENGTH + " characters");
        }
        if (command.indexOf('\n') >= 0 || command.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("command must be a single line");
        }
        if (process == null || !process.isAlive() || processInput == null) {
            throw new IllegalStateException("Forge instance is not running");
        }
        processInput.println(command);
        if (processInput.checkError()) {
            throw new IllegalStateException("Could not write to the Forge console");
        }
    }

    @Override
    public void reload(Duration timeout) throws Exception {
        requirePositive(timeout);
        long deadline = System.nanoTime() + timeout.toNanos();
        if (!shutdown(null, remaining(deadline))) {
            throw new IOException("Forge instance did not stop cleanly before reload");
        }
        start(remaining(deadline));
    }

    @Override
    public boolean shutdown(String marker, Duration timeout) throws InterruptedException {
        requirePositive(timeout);
        Process running;
        synchronized (this) {
            running = process;
            if (running == null) {
                return false;
            }
            if (running.isAlive()) {
                sendCommand("stop", timeout);
            }
        }

        long deadline = System.nanoTime() + timeout.toNanos();
        boolean observed = marker == null || awaitLog(marker, remaining(deadline));
        long remainingNanos = Math.max(1, deadline - System.nanoTime());
        boolean exited = running.waitFor(remainingNanos, TimeUnit.NANOSECONDS);
        Thread reader;
        synchronized (this) {
            reader = outputReader;
        }
        if (exited && reader != null) {
            reader.join(Duration.ofNanos(Math.max(1, deadline - System.nanoTime())));
        }
        return observed && exited && running.exitValue() == 0;
    }

    @Override
    public List<Path> artifacts() {
        LinkedHashSet<Path> artifacts;
        synchronized (outputMonitor) {
            artifacts = new LinkedHashSet<>(sessionLogs);
        }
        addIfFile(artifacts, instance.resolve("logs/latest.log"));
        Path crashes = instance.resolve("crash-reports");
        if (Files.isDirectory(crashes, LinkOption.NOFOLLOW_LINKS)) {
            try (var files = Files.list(crashes)) {
                files.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                        .sorted(Comparator.comparing(Path::toString)).forEach(artifacts::add);
            } catch (IOException ignored) {
                // The process transcript remains available if Forge's crash directory cannot be read.
            }
        }
        return List.copyOf(artifacts);
    }

    @Override
    public void close() {
        Process running;
        synchronized (this) {
            running = process;
            if (processInput != null) {
                processInput.close();
            }
        }
        if (running != null && running.isAlive()) {
            running.destroy();
            try {
                if (!running.waitFor(2, TimeUnit.SECONDS)) {
                    running.destroyForcibly();
                    running.waitFor(2, TimeUnit.SECONDS);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                running.destroyForcibly();
            }
        }
    }

    private void readOutput(Process launched, Path sessionLog, int generation) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(launched.getInputStream(), StandardCharsets.UTF_8));
                var writer = Files.newBufferedWriter(sessionLog, StandardCharsets.UTF_8,
                        StandardOpenOption.APPEND, StandardOpenOption.WRITE)) {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
                writer.flush();
                synchronized (outputMonitor) {
                    if (generation == launchNumber) {
                        currentOutput.add(line);
                        outputMonitor.notifyAll();
                    }
                }
            }
        } catch (IOException exception) {
            synchronized (outputMonitor) {
                if (generation == launchNumber) {
                    currentOutput.add("LB-SCENARIO-OUTPUT-001: " + exception.getMessage());
                }
            }
        } finally {
            synchronized (outputMonitor) {
                if (generation == launchNumber) {
                    outputEnded = true;
                    outputMonitor.notifyAll();
                }
            }
        }
    }

    private static List<String> forgeLaunchCommand(Path instance, BridgeEnvironment side) throws IOException {
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        String scriptName = switch (side) {
            case SERVER -> windows ? "run.bat" : "run.sh";
            case CLIENT -> windows ? "run-client.bat" : "run-client.sh";
        };
        Path script = instance.resolve(scriptName);
        if (!Files.isRegularFile(script, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Missing Forge launch script: " + script);
        }
        if (windows) {
            return side == BridgeEnvironment.SERVER
                    ? List.of("cmd.exe", "/c", script.toString(), "nogui")
                    : List.of("cmd.exe", "/c", script.toString());
        }
        return side == BridgeEnvironment.SERVER ? List.of("sh", script.toString(), "nogui")
                : List.of("sh", script.toString());
    }

    private static void prependCurrentJava(ProcessBuilder builder) {
        String javaBin = Path.of(System.getProperty("java.home"), "bin").toString();
        String oldPath = builder.environment().getOrDefault("PATH", "");
        builder.environment().put("PATH", javaBin + java.io.File.pathSeparator + oldPath);
        builder.environment().put("JAVA_HOME", System.getProperty("java.home"));
    }

    private static Duration remaining(long deadline) {
        long nanos = deadline - System.nanoTime();
        if (nanos <= 0) {
            throw new IllegalArgumentException("scenario operation timed out");
        }
        return Duration.ofNanos(nanos);
    }

    private static void requirePositive(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
    }

    private static void addIfFile(LinkedHashSet<Path> artifacts, Path path) {
        if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            artifacts.add(path);
        }
    }

    @FunctionalInterface
    interface CommandFactory {
        List<String> create(Path instance) throws IOException;
    }
}
