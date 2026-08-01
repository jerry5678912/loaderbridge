package dev.loaderbridge.fabric.remap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.loaderbridge.api.AdapterDescriptor;
import dev.loaderbridge.api.BridgeAdapter;
import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.BridgePhase;
import dev.loaderbridge.api.BridgePlan;
import dev.loaderbridge.api.BridgeRequest;
import dev.loaderbridge.api.Diagnostic;
import dev.loaderbridge.api.DiagnosticSeverity;
import dev.loaderbridge.api.LoaderId;
import dev.loaderbridge.api.ModInspection;
import dev.loaderbridge.api.PreparationResult;
import dev.loaderbridge.fabric.metadata.FabricDependencyResolver;
import dev.loaderbridge.fabric.metadata.FabricEntrypoint;
import dev.loaderbridge.fabric.metadata.FabricModInspector;
import dev.loaderbridge.fabric.metadata.FabricModMetadata;
import dev.loaderbridge.fabric.metadata.FabricModTree;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** First directional adapter. It currently accepts namespace-neutral fixtures and reports remapping gaps. */
public final class FabricToForgeAdapter implements BridgeAdapter {
    private static final LoaderId FABRIC = new LoaderId("fabric");
    private static final LoaderId FORGE = new LoaderId("forge");
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping()
            .registerTypeHierarchyAdapter(Path.class,
                    (com.google.gson.JsonSerializer<Path>) (source, type, context) ->
                            new com.google.gson.JsonPrimitive(source.toString()))
            .create();
    private final FabricModInspector inspector = new FabricModInspector();
    private final BytecodeReferenceAnalyzer analyzer = new BytecodeReferenceAnalyzer();

    @Override
    public AdapterDescriptor descriptor() {
        return new AdapterDescriptor("fabric-to-forge", "1", FABRIC, FORGE, "=1.21.1", "[52.1.0,53)",
                List.of(BridgeCapability.METADATA, BridgeCapability.DEPENDENCY_RESOLUTION));
    }

    @Override
    public ModInspection inspect(Path artifact) throws IOException {
        FabricModMetadata metadata = inspector.inspect(artifact).root();
        return inspection(artifact, metadata, List.of());
    }

    @Override
    public BridgePlan plan(BridgeRequest request) throws IOException {
        List<Diagnostic> diagnostics = new ArrayList<>();
        List<ModInspection> inspections = new ArrayList<>();
        List<FabricModMetadata> allMetadata = new ArrayList<>();
        EnumSet<BridgeCapability> required = EnumSet.of(BridgeCapability.METADATA,
                BridgeCapability.DEPENDENCY_RESOLUTION);

        validateTarget(request, diagnostics);
        for (Path artifact : request.inputArtifacts()) {
            try {
                FabricModTree tree = inspector.inspect(artifact);
                collectMetadata(tree, allMetadata);
                FabricModMetadata metadata = tree.root();
                ReferenceInventory inventory = analyzer.analyze(artifact);
                analyzeRequirements(artifact, metadata, inventory, required, diagnostics, request);
                inspections.add(inspection(artifact, metadata, List.of()));
            } catch (IOException exception) {
                diagnostics.add(error("LB-INSPECT-001", BridgePhase.INSPECT, null, artifact,
                        "Could not inspect Fabric mod", exception));
            }
        }
        diagnostics.addAll(new FabricDependencyResolver().resolve(null, allMetadata,
                Map.of("minecraft", request.minecraftVersion(), "java", Runtime.version().feature() + ".0.0",
                        "fabricloader", "0.16.10")));
        return new BridgePlan(descriptor(), inspections, List.copyOf(required), diagnostics);
    }

    @Override
    public PreparationResult prepare(BridgeRequest request, BridgePlan plan) throws IOException {
        if (!plan.canPrepare()) {
            return new PreparationResult(List.of(), writeReport(request, plan, List.of()), plan.diagnostics());
        }
        Files.createDirectories(request.outputDirectory());
        Files.createDirectories(request.cacheDirectory());
        List<Path> outputs = new ArrayList<>();
        List<PreparedArtifact> prepared = new ArrayList<>();
        DeterministicJarPreparer preparer = new DeterministicJarPreparer();
        for (Path source : request.inputArtifacts()) {
            FabricModMetadata metadata = inspector.inspect(source).root();
            String sourceHash = sha256(source);
            String cacheKey = sha256((sourceHash + "|0.1.0|" + request.minecraftVersion() + "|"
                    + request.hostVersion()).getBytes(StandardCharsets.UTF_8));
            Path cached = request.cacheDirectory().resolve(cacheKey + ".jar");
            if (!Files.exists(cached)) {
                preparer.prepare(source, cached, metadata,
                        PreparationManifest.pinned(request.minecraftVersion(), request.hostVersion()));
            }
            Path output = request.outputDirectory().resolve(metadata.id() + "-" + safe(metadata.version())
                    + "-loaderbridge.jar");
            Files.copy(cached, output, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            outputs.add(output);
            prepared.add(new PreparedArtifact(metadata.id(), source.toString(), sourceHash,
                    output.toString(), sha256(output), cacheKey));
        }
        Path report = writeReport(request, plan, prepared);
        writeLock(request, prepared);
        return new PreparationResult(outputs, report, plan.diagnostics());
    }

    private void analyzeRequirements(Path artifact, FabricModMetadata metadata, ReferenceInventory inventory,
            Set<BridgeCapability> required, List<Diagnostic> diagnostics, BridgeRequest request) {
        if (!metadata.mixins().isEmpty()) {
            required.add(BridgeCapability.MIXINS);
            diagnostics.add(unsupported("LB-MIXIN-001", metadata.id(), artifact,
                    "Mixin registration is not implemented in this scaffold build"));
        }
        if (metadata.accessWidener().isPresent()) {
            required.add(BridgeCapability.ACCESS_WIDENERS);
            diagnostics.add(unsupported("LB-AW-001", metadata.id(), artifact,
                    "Access-widener transformation is not implemented in this scaffold build"));
        }
        if (!metadata.nestedJars().isEmpty()) {
            required.add(BridgeCapability.NESTED_JARS);
            diagnostics.add(unsupported("LB-NESTED-001", metadata.id(), artifact,
                    "Nested JARs are inspected but recursive transformation/loading is not implemented"));
        }
        if (!metadata.languageAdapters().isEmpty() || metadata.entrypoints().values().stream()
                .flatMap(List::stream).map(FabricEntrypoint::adapter).anyMatch(adapter -> !adapter.equals("default"))) {
            diagnostics.add(unsupported("LB-LANG-001", metadata.id(), artifact,
                    "Custom Fabric language adapters are not supported"));
        }
        if (!inventory.fabricApiClasses().isEmpty()) {
            required.add(BridgeCapability.FABRIC_API);
            diagnostics.add(unsupported("LB-FAPI-001", metadata.id(), artifact,
                    "Unbridged Fabric API references: " + inventory.fabricApiClasses().stream().limit(5).toList()));
        }
        if (!inventory.loaderApiClasses().isEmpty()) {
            required.add(BridgeCapability.LOADER_API);
            diagnostics.add(unsupported("LB-LOADER-001", metadata.id(), artifact,
                    "Loader API references require compatibility verification against the current shim subset: "
                            + inventory.loaderApiClasses().stream().limit(5).toList()));
        }
        if (!inventory.nativeLibraries().isEmpty()) {
            diagnostics.add(unsupported("LB-NATIVE-001", metadata.id(), artifact,
                    "Native libraries require manual compatibility review: " + inventory.nativeLibraries()));
        }
        if (!inventory.minecraftClasses().isEmpty()
                && request.sourceNamespaceOverride().map(value -> !value.equals("official")).orElse(true)) {
            required.add(BridgeCapability.REMAPPING);
            diagnostics.add(unsupported("LB-REMAP-001", metadata.id(), artifact,
                    "Minecraft bytecode references require the pending intermediary-to-official remapper"));
        }
    }

    private static void validateTarget(BridgeRequest request, List<Diagnostic> diagnostics) {
        if (!request.minecraftVersion().equals("1.21.1") || !request.hostLoader().equals(FORGE)
                || !request.hostVersion().startsWith("52.1.")) {
            diagnostics.add(error("LB-PLAN-001", BridgePhase.PLAN, null, null,
                    "This adapter requires Minecraft 1.21.1 and Forge 52.1.x", null));
        }
    }

    private static ModInspection inspection(Path artifact, FabricModMetadata metadata,
            List<Diagnostic> diagnostics) {
        Map<String, List<String>> entrypoints = new LinkedHashMap<>();
        metadata.entrypoints().forEach((key, values) -> entrypoints.put(key,
                values.stream().map(FabricEntrypoint::value).toList()));
        return new ModInspection(artifact, FABRIC, metadata.id(), metadata.version(), metadata.environment(),
                entrypoints, diagnostics);
    }

    private static void collectMetadata(FabricModTree tree, List<FabricModMetadata> destination) {
        destination.add(tree.root());
        tree.nested().forEach(child -> collectMetadata(child, destination));
    }

    private static Diagnostic unsupported(String code, String modId, Path artifact, String message) {
        return new Diagnostic(DiagnosticSeverity.ERROR, code, BridgePhase.PLAN, modId, artifact, message, null);
    }

    private static Diagnostic error(String code, BridgePhase phase, String modId, Path artifact,
            String message, Exception cause) {
        return new Diagnostic(DiagnosticSeverity.ERROR, code, phase, modId, artifact, message,
                cause == null ? null : cause.getClass().getSimpleName() + ": " + cause.getMessage());
    }

    private static String safe(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String sha256(Path path) throws IOException {
        return sha256(Files.readAllBytes(path));
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by Java", exception);
        }
    }

    private static Path writeReport(BridgeRequest request, BridgePlan plan, List<PreparedArtifact> prepared)
            throws IOException {
        Files.createDirectories(request.outputDirectory());
        Path report = request.outputDirectory().resolve("compatibility-report.json");
        Files.writeString(report, JSON.toJson(new CompatibilityReport("1", plan.adapter(), plan.mods(),
                plan.requiredCapabilities(), plan.diagnostics(), prepared)), StandardCharsets.UTF_8);
        return report;
    }

    private static void writeLock(BridgeRequest request, List<PreparedArtifact> prepared) throws IOException {
        Path lock = request.outputDirectory().resolve("bridge.lock.json");
        LockData data = new LockData("1", request.minecraftVersion(), "forge", request.hostVersion(),
                "fabric-to-forge", "0.1.0", prepared);
        Files.writeString(lock, JSON.toJson(data), StandardCharsets.UTF_8);
    }

    private record PreparedArtifact(String modId, String source, String sourceSha256, String output,
            String outputSha256, String cacheKey) {}

    private record CompatibilityReport(String formatVersion, AdapterDescriptor adapter,
            List<ModInspection> mods, List<BridgeCapability> requiredCapabilities,
            List<Diagnostic> diagnostics, List<PreparedArtifact> preparedArtifacts) {}

    private record LockData(String formatVersion, String minecraftVersion, String hostLoader,
            String hostVersion, String adapter, String adapterVersion, List<PreparedArtifact> artifacts) {}
}
