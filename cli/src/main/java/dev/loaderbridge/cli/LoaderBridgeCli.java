package dev.loaderbridge.cli;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.loaderbridge.api.BridgeAdapter;
import dev.loaderbridge.api.BridgeEnvironment;
import dev.loaderbridge.api.BridgeRequest;
import dev.loaderbridge.api.Diagnostic;
import dev.loaderbridge.api.DiagnosticSeverity;
import dev.loaderbridge.api.LoaderId;
import dev.loaderbridge.api.repository.RepositoryProvider;
import dev.loaderbridge.catalog.CatalogCollector;
import dev.loaderbridge.catalog.CatalogSnapshotCodec;
import dev.loaderbridge.catalog.RepositoryDependencyResolver;
import dev.loaderbridge.catalog.RepositoryResolutionLockCodec;
import dev.loaderbridge.integration.ForgeServerVerifier;
import dev.loaderbridge.integration.ForgeProcessScenarioSession;
import dev.loaderbridge.integration.ScenarioRunner;
import dev.loaderbridge.scenario.ScenarioExecutionContext;
import dev.loaderbridge.scenario.ScenarioPlugin;
import dev.loaderbridge.scenario.ScenarioRunResult;
import dev.loaderbridge.scenario.yaml.ScenarioYamlParser;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
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
                LoaderBridgeCli.Verify.class, LoaderBridgeCli.Catalog.class,
                LoaderBridgeCli.Resolve.class, LoaderBridgeCli.TestScenario.class})
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

        @Option(names = "--expect-marker", description = "Require an output marker (repeatable).")
        List<String> expectedMarkers = new ArrayList<>();

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
                        System.out::println, expectedMarkers);
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

    @Command(name = "catalog", description = "Manage measured compatibility catalogs.",
            subcommands = Catalog.Freeze.class)
    static final class Catalog implements Runnable {
        @Override
        public void run() {
            CommandLine.usage(this, System.out);
        }

        @Command(name = "freeze", description = "Resolve and freeze a deterministic catalog snapshot.")
        static final class Freeze implements Callable<Integer> {
            @Option(names = "--snapshot-id", required = true)
            String snapshotId;

            @Option(names = "--frozen-at", required = true,
                    description = "Immutable input timestamp in ISO-8601 form.")
            String frozenAt;

            @Option(names = "--output", required = true)
            Path output;

            @Option(names = "--target", defaultValue = "1000")
            int target;

            @Option(names = "--per-repository", defaultValue = "500")
            int perRepository;

            @Override
            public Integer call() {
                if (target < 1 || perRepository < 1 || perRepository > target) {
                    System.err.println("Catalog target and per-repository quota are invalid");
                    return INVALID_INPUT;
                }
                java.time.Instant timestamp;
                try {
                    timestamp = java.time.Instant.parse(frozenAt);
                } catch (java.time.format.DateTimeParseException exception) {
                    System.err.println("Invalid --frozen-at timestamp: " + frozenAt);
                    return INVALID_INPUT;
                }
                List<RepositoryProvider> providers = ServiceLoader.load(RepositoryProvider.class).stream()
                        .map(ServiceLoader.Provider::get).toList();
                if (providers.isEmpty()) {
                    System.err.println("No repository providers are installed");
                    return UNSUPPORTED;
                }
                try {
                    var snapshot = new CatalogCollector(providers).collectAndFreeze(target,
                            perRepository, snapshotId, timestamp);
                    new CatalogSnapshotCodec().write(snapshot, output);
                    System.out.println("Frozen " + snapshot.entries().size() + " projects to " + output);
                    return 0;
                } catch (IOException exception) {
                    System.err.println("Catalog freeze failed: " + exception.getMessage());
                    return UNSUPPORTED;
                }
            }
        }
    }

    @Command(name = "resolve", description = "Resolve and install a project plus required dependencies.")
    static final class Resolve implements Callable<Integer> {
        @Option(names = "--project", required = true,
                description = "Repository-qualified project ID, such as modrinth:AABBCCDD.")
        String project;

        @Option(names = "--output", required = true)
        Path output;

        @Override
        public Integer call() {
            String[] identity = project.split(":", -1);
            if (identity.length != 2 || identity[0].isBlank() || identity[1].isBlank()) {
                System.err.println("Project must be repository-qualified, for example modrinth:AABBCCDD");
                return INVALID_INPUT;
            }
            List<RepositoryProvider> providers = ServiceLoader.load(RepositoryProvider.class).stream()
                    .map(ServiceLoader.Provider::get).toList();
            RepositoryProvider provider = providers.stream()
                    .filter(candidate -> candidate.id().value().equals(identity[0])).findFirst().orElse(null);
            if (provider == null) {
                System.err.println("No repository provider is installed for " + identity[0]);
                return UNSUPPORTED;
            }
            try {
                var root = provider.versions(identity[1], "1.21.1", "fabric").stream()
                        .filter(dev.loaderbridge.api.repository.RepositoryArtifact::isEligibleFabric1211)
                        .max(Comparator.comparing(
                                dev.loaderbridge.api.repository.RepositoryArtifact::publishedAt)
                                .thenComparing(
                                        dev.loaderbridge.api.repository.RepositoryArtifact::versionId))
                        .orElseThrow(() -> new IOException("No eligible Fabric 1.21.1 release found"));
                RepositoryDependencyResolver resolver = new RepositoryDependencyResolver(providers);
                var graph = resolver.resolveRequired(List.of(root));
                var cached = resolver.downloadAll(graph, output.resolve(".cache"));
                Path mods = Files.createDirectories(output.resolve("mods")).toAbsolutePath().normalize();
                for (var artifact : graph.installationOrder()) {
                    Path source = cached.get(artifact);
                    Path destination = mods.resolve(artifact.fileName()).normalize();
                    if (!destination.startsWith(mods)) {
                        throw new IOException("Unsafe resolved artifact path");
                    }
                    if (Files.exists(destination) && Files.mismatch(source, destination) != -1) {
                        throw new IOException("Resolved artifacts collide at " + artifact.fileName());
                    }
                    Files.copy(source, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                Path lock = output.resolve("bridge.repository.lock.json");
                new RepositoryResolutionLockCodec().write(root, graph, lock);
                System.out.println("Resolved " + graph.installationOrder().size() + " artifact(s) to " + mods);
                System.out.println("Lock: " + lock);
                return 0;
            } catch (IOException exception) {
                System.err.println("Repository resolution failed: " + exception.getMessage());
                return UNSUPPORTED;
            }
        }
    }

    @Command(name = "test", description = "Run a behavioral compatibility scenario against a Forge instance.")
    static final class TestScenario implements Callable<Integer> {
        @Option(names = "--scenario", required = true)
        Path scenarioFile;

        @Option(names = "--instance", required = true)
        Path instance;

        @Option(names = "--artifacts", required = true)
        Path artifacts;

        @Option(names = "--json", description = "Emit the result as JSON.")
        boolean json;

        @Override
        public Integer call() {
            if (!Files.isDirectory(instance)) {
                System.err.println("Instance is not a directory: " + instance);
                return INVALID_INPUT;
            }
            try {
                var scenario = new ScenarioYamlParser().parse(scenarioFile);
                if (scenario.side() != BridgeEnvironment.SERVER) {
                    System.err.println("LB-SCENARIO-CLIENT-001: client scenario execution is not implemented");
                    return LAUNCH_FAILURE;
                }
                Files.createDirectories(artifacts);
                if (!Files.isDirectory(artifacts)) {
                    System.err.println("Artifacts path is not a directory: " + artifacts);
                    return INVALID_INPUT;
                }
                var context = new ScenarioExecutionContext(instance, artifacts, scenario.side(), Map.of());
                List<ScenarioPlugin> plugins = ServiceLoader.load(ScenarioPlugin.class).stream()
                        .map(ServiceLoader.Provider::get).toList();
                ScenarioRunResult result;
                try (var session = new ForgeProcessScenarioSession(instance, artifacts)) {
                    result = new ScenarioRunner(plugins).run(scenario, context, session);
                }
                String encoded = JSON.toJson(report(result));
                Path report = artifacts.resolve("scenario-report.json");
                Files.writeString(report, encoded + System.lineSeparator(),
                        java.nio.charset.StandardCharsets.UTF_8);
                if (json) {
                    System.out.println(encoded);
                } else {
                    result.steps().forEach(step -> System.out.printf("%s %s: %s%n",
                            step.status(), step.code(), step.message()));
                    System.out.println("Report: " + report);
                }
                if (result.succeeded()) {
                    return 0;
                }
                boolean unsupportedAction = result.steps().stream()
                        .anyMatch(step -> step.code().equals("LB-SCENARIO-ACTION-001"));
                return unsupportedAction ? UNSUPPORTED : LAUNCH_FAILURE;
            } catch (IOException | IllegalArgumentException exception) {
                System.err.println("Invalid scenario input: " + exception.getMessage());
                return INVALID_INPUT;
            }
        }

        private static Map<String, Object> report(ScenarioRunResult result) {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("schemaVersion", 1);
            report.put("scenarioId", result.scenarioId());
            report.put("succeeded", result.succeeded());
            report.put("failurePhase", result.failurePhase().map(Enum::name).orElse(null));
            report.put("steps", result.steps().stream().map(step -> {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("status", step.status().name());
                value.put("failurePhase", step.failurePhase().map(Enum::name).orElse(null));
                value.put("code", step.code());
                value.put("message", step.message());
                value.put("elapsedMillis", step.elapsed().toMillis());
                value.put("artifacts", step.artifacts().stream().map(Path::toString).toList());
                return value;
            }).toList());
            return report;
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
