package dev.loaderbridge.cli;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.loaderbridge.api.BridgeAdapter;
import dev.loaderbridge.api.BridgeEnvironment;
import dev.loaderbridge.api.BridgeRequest;
import dev.loaderbridge.api.Diagnostic;
import dev.loaderbridge.api.DiagnosticSeverity;
import dev.loaderbridge.api.LoaderId;
import dev.loaderbridge.integration.ForgeServerVerifier;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "loaderbridge", mixinStandardHelpOptions = true,
        description = "Experimental Fabric-to-Forge compatibility scaffold.",
        subcommands = {LoaderBridgeCli.Inspect.class, LoaderBridgeCli.Prepare.class,
                LoaderBridgeCli.Verify.class})
public final class LoaderBridgeCli implements Runnable {
    static final int INVALID_INPUT = 2;
    static final int UNSUPPORTED = 3;
    static final int TRANSFORMATION_FAILURE = 4;
    static final int LAUNCH_FAILURE = 5;
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping()
            .registerTypeHierarchyAdapter(Path.class,
                    (com.google.gson.JsonSerializer<Path>) (source, type, context) ->
                            new com.google.gson.JsonPrimitive(source.toString()))
            .create();

    public static void main(String[] args) {
        int exitCode = new CommandLine(new LoaderBridgeCli()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    @Command(name = "inspect", description = "Inspect a JAR by its contents.")
    static final class Inspect implements Callable<Integer> {
        @Parameters(index = "0", paramLabel = "JAR")
        Path artifact;

        @Option(names = "--json", description = "Emit JSON.")
        boolean json;

        @Override
        public Integer call() {
            if (!Files.isRegularFile(artifact)) {
                System.err.println("Input is not a readable file: " + artifact);
                return INVALID_INPUT;
            }
            List<Object> results = new ArrayList<>();
            for (BridgeAdapter adapter : adapters()) {
                try {
                    results.add(adapter.inspect(artifact));
                } catch (IOException ignored) {
                    // A directional adapter is allowed to reject an artifact it does not own.
                }
            }
            if (results.isEmpty()) {
                System.err.println("No installed adapter recognized " + artifact);
                return UNSUPPORTED;
            }
            if (json) {
                System.out.println(JSON.toJson(results.size() == 1 ? results.getFirst() : results));
            } else {
                results.forEach(System.out::println);
            }
            return 0;
        }
    }

    @Command(name = "prepare", description = "Plan and prepare mods into a separate output directory.")
    static final class Prepare implements Callable<Integer> {
        @Option(names = "--minecraft", required = true)
        String minecraft;

        @Option(names = "--host", required = true)
        String host;

        @Option(names = "--forge-version", required = true)
        String forgeVersion;

        @Option(names = "--mods", required = true)
        Path mods;

        @Option(names = "--output", required = true)
        Path output;

        @Option(names = "--side", defaultValue = "client", converter = EnvironmentConverter.class)
        BridgeEnvironment side;

        @Option(names = "--source-namespace")
        Optional<String> sourceNamespace = Optional.empty();

        @Option(names = "--refresh", description = "Refresh checksum-verified Mojang metadata and artifacts.")
        boolean refresh;

        @Override
        public Integer call() {
            if (!Files.isDirectory(mods)) {
                System.err.println("Mods path is not a directory: " + mods);
                return INVALID_INPUT;
            }
            String resolvedForge = switch (forgeVersion) {
                case "recommended" -> "52.1.0";
                case "latest" -> "52.1.16";
                default -> forgeVersion;
            };
            List<Path> artifacts;
            try (var paths = Files.list(mods)) {
                artifacts = paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(java.util.Locale.ROOT)
                                .endsWith(".jar"))
                        .sorted(Comparator.comparing(Path::toString)).toList();
            } catch (IOException exception) {
                System.err.println("Could not list mods: " + exception.getMessage());
                return INVALID_INPUT;
            }
            if (artifacts.isEmpty()) {
                System.err.println("No JARs found in " + mods);
                return INVALID_INPUT;
            }
            BridgeAdapter adapter = findAdapter("fabric", host).orElse(null);
            if (adapter == null) {
                System.err.println("No Fabric-to-" + host + " adapter is installed");
                return UNSUPPORTED;
            }
            BridgeRequest request = new BridgeRequest(minecraft, new LoaderId(host), resolvedForge, side,
                    artifacts, output, output.resolve(".cache"), sourceNamespace, refresh);
            try {
                var plan = adapter.plan(request);
                printDiagnostics(plan.diagnostics(), new PrintWriter(System.err, true));
                if (!plan.canPrepare()) {
                    adapter.prepare(request, plan);
                    return UNSUPPORTED;
                }
                var result = adapter.prepare(request, plan);
                System.out.println("Prepared " + result.artifacts().size() + " artifact(s)");
                System.out.println("Report: " + result.report());
                return result.succeeded() ? 0 : TRANSFORMATION_FAILURE;
            } catch (IOException | RuntimeException exception) {
                System.err.println("Transformation failed: " + exception.getMessage());
                return TRANSFORMATION_FAILURE;
            }
        }
    }

    @Command(name = "verify", description = "Launch and validate a prepared Forge instance.")
    static final class Verify implements Callable<Integer> {
        @Option(names = "--instance", required = true)
        Path instance;

        @Option(names = "--side", required = true, converter = EnvironmentConverter.class)
        BridgeEnvironment side;

        @Option(names = "--timeout-seconds", defaultValue = "120")
        long timeoutSeconds;

        @Override
        public Integer call() {
            if (!Files.isDirectory(instance)) {
                System.err.println("Instance is not a directory: " + instance);
                return INVALID_INPUT;
            }
            if (side != BridgeEnvironment.SERVER) {
                System.err.println("LB-VERIFY-001: client launch verification is not implemented");
                return LAUNCH_FAILURE;
            }
            if (timeoutSeconds <= 0) {
                System.err.println("Timeout must be positive");
                return INVALID_INPUT;
            }
            try {
                var result = new ForgeServerVerifier().verify(instance, Duration.ofSeconds(timeoutSeconds),
                        System.out::println);
                if (result.succeeded()) {
                    System.out.println(result.diagnosticCode() + ": " + result.message());
                    return 0;
                }
                System.err.println(result.diagnosticCode() + ": " + result.message());
                return LAUNCH_FAILURE;
            } catch (IOException exception) {
                System.err.println("LB-VERIFY-002: " + exception.getMessage());
                return LAUNCH_FAILURE;
            }
        }
    }

    private static List<BridgeAdapter> adapters() {
        return ServiceLoader.load(BridgeAdapter.class).stream()
                .map(ServiceLoader.Provider::get)
                .sorted(Comparator.comparing(adapter -> adapter.descriptor().id()))
                .toList();
    }

    private static Optional<BridgeAdapter> findAdapter(String source, String target) {
        return adapters().stream().filter(adapter -> adapter.descriptor().sourceLoader().value().equals(source)
                && adapter.descriptor().targetLoader().value().equals(target)).findFirst();
    }

    private static void printDiagnostics(List<Diagnostic> diagnostics, PrintWriter output) {
        diagnostics.forEach(diagnostic -> output.printf("%s %s [%s] %s%n", diagnostic.severity(),
                diagnostic.code(), diagnostic.phase(), diagnostic.message()));
        if (diagnostics.stream().anyMatch(item -> item.severity() == DiagnosticSeverity.ERROR)) {
            output.flush();
        }
    }

    static final class EnvironmentConverter implements CommandLine.ITypeConverter<BridgeEnvironment> {
        @Override
        public BridgeEnvironment convert(String value) {
            return BridgeEnvironment.valueOf(value.toUpperCase(java.util.Locale.ROOT));
        }
    }
}
