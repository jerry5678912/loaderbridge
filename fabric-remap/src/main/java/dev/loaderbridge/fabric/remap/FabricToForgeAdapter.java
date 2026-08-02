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
import dev.loaderbridge.fabric.metadata.JarReadLimits;
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
import java.util.LinkedHashSet;
import java.util.zip.ZipFile;

/** First directional adapter for Fabric intermediary bytecode targeting Forge's official-name runtime. */
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
    private final MinecraftArtifactsProvider minecraftArtifacts;
    private final IntermediaryMappingsProvider intermediaryMappings;
    private final RuntimeLibraryProvider mixinExtrasRuntime;

    public FabricToForgeAdapter() {
        this(new MinecraftArtifactResolver(), new BundledIntermediaryMappings(),
                new MixinExtrasRuntimeResolver());
    }

    FabricToForgeAdapter(MinecraftArtifactsProvider minecraftArtifacts,
            IntermediaryMappingsProvider intermediaryMappings) {
        this(minecraftArtifacts, intermediaryMappings, new MixinExtrasRuntimeResolver());
    }

    FabricToForgeAdapter(MinecraftArtifactsProvider minecraftArtifacts,
            IntermediaryMappingsProvider intermediaryMappings,
            RuntimeLibraryProvider mixinExtrasRuntime) {
        this.minecraftArtifacts = java.util.Objects.requireNonNull(minecraftArtifacts, "minecraftArtifacts");
        this.intermediaryMappings = java.util.Objects.requireNonNull(intermediaryMappings, "intermediaryMappings");
        this.mixinExtrasRuntime = java.util.Objects.requireNonNull(mixinExtrasRuntime, "mixinExtrasRuntime");
    }

    @Override
    public AdapterDescriptor descriptor() {
        return new AdapterDescriptor("fabric-to-forge", "1", FABRIC, FORGE, "=1.21.1", "[52.1.0,53)",
                List.of(BridgeCapability.METADATA, BridgeCapability.DEPENDENCY_RESOLUTION,
                        BridgeCapability.REMAPPING, BridgeCapability.MIXINS,
                        BridgeCapability.MIXIN_EXTRAS));
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
        diagnoseDuplicateIds(allMetadata, diagnostics);
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
        ResolvedMinecraftArtifacts resolvedMinecraft = null;
        Path resolvedIntermediaryMappings = null;
        List<PreparationInput> inputs = new ArrayList<>();
        Set<String> seenArtifacts = new LinkedHashSet<>();
        Map<String, String> seenModVersions = new LinkedHashMap<>();
        String adapterFingerprint = implementationFingerprint();
        boolean needsMixinExtras = false;
        for (Path source : request.inputArtifacts()) {
            collectPreparationInputs(source, source.toString(), null, null,
                    request.cacheDirectory(), inputs, seenArtifacts, seenModVersions);
        }
        for (PreparationInput input : inputs) {
            Path source = input.path();
            FabricModMetadata metadata = input.metadata();
            ReferenceInventory inventory = analyzer.analyze(source);
            needsMixinExtras |= !inventory.mixinExtrasClasses().isEmpty();
            SourceNamespace namespace = sourceNamespace(request, inventory, null, metadata.id(), source);
            String sourceHash = sha256(source);
            Path preparationInput = source;
            Path runtimeMappings = null;
            String mappingKey = "namespace-neutral";
            boolean needsMappingResolver = inventory.loaderApiClasses().contains(
                    "net.fabricmc.loader.api.MappingResolver");
            if (namespace == SourceNamespace.INTERMEDIARY || needsMappingResolver) {
                if (resolvedMinecraft == null) {
                    resolvedMinecraft = minecraftArtifacts.resolve(request.minecraftVersion(),
                            request.cacheDirectory(), request.refresh());
                    resolvedIntermediaryMappings = intermediaryMappings.resolve(request.minecraftVersion(),
                            request.cacheDirectory());
                }
                mappingKey = resolvedMinecraft.clientJar().sha1() + "|"
                        + resolvedMinecraft.clientMappings().sha1() + "|"
                        + sha256(resolvedIntermediaryMappings);
                MinecraftRemappingPipeline pipeline = new MinecraftRemappingPipeline();
                Path work = request.cacheDirectory().resolve("remap-work");
                if (namespace == SourceNamespace.INTERMEDIARY) {
                    Path remapped = request.cacheDirectory().resolve("remapped-inputs")
                            .resolve(sourceHash + "-named.jar");
                    runtimeMappings = pipeline.remap(source, remapped,
                            resolvedMinecraft.clientJar().path(), resolvedIntermediaryMappings,
                            resolvedMinecraft.clientMappings().path(), work);
                    preparationInput = remapped;
                } else {
                    runtimeMappings = pipeline.composeMappings(resolvedIntermediaryMappings,
                            resolvedMinecraft.clientMappings().path(), work);
                }
            }
            String containmentKey = input.parentModId() == null ? "root"
                    : input.parentModId() + "!/" + input.parentSubLocation();
            String cacheKey = sha256((sourceHash + "|" + FabricAdapterVersion.CURRENT + "|"
                    + adapterFingerprint + "|" + request.minecraftVersion() + "|"
                    + request.hostVersion() + "|" + mappingKey + "|" + containmentKey)
                    .getBytes(StandardCharsets.UTF_8));
            Path cached = request.cacheDirectory().resolve(cacheKey + ".jar");
            if (!Files.exists(cached)) {
                PreparationManifest manifest = PreparationManifest.pinned(
                        request.minecraftVersion(), request.hostVersion())
                        .namespaces(namespace.name().toLowerCase(java.util.Locale.ROOT), "official");
                if (input.parentModId() != null) {
                    manifest = manifest.nested(input.parentModId(), input.parentSubLocation());
                }
                preparer.prepare(preparationInput, cached, metadata, manifest, runtimeMappings);
            }
            Path output = request.outputDirectory().resolve(metadata.id() + "-" + safe(metadata.version())
                    + "-loaderbridge.jar");
            Files.copy(cached, output, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            outputs.add(output);
            prepared.add(new PreparedArtifact(metadata.id(), input.source(), sourceHash,
                    output.toString(), sha256(output), cacheKey, namespace.name().toLowerCase(
                            java.util.Locale.ROOT)));
        }
        if (needsMixinExtras) {
            ResolvedRuntimeLibrary library = mixinExtrasRuntime.resolve(
                    request.cacheDirectory(), request.refresh());
            Path output = request.outputDirectory().resolve(
                    library.id() + "-" + library.version() + ".jar");
            Files.copy(library.path(), output, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            outputs.add(output);
            prepared.add(new PreparedArtifact(library.id(), library.url().toString(), library.sha256(),
                    output.toString(), sha256(output), library.sha256(), "runtime-library"));
        }
        Path report = writeReport(request, plan, prepared);
        writeLock(request, prepared, resolvedMinecraft, resolvedIntermediaryMappings,
                adapterFingerprint);
        return new PreparationResult(outputs, report, plan.diagnostics());
    }

    private void analyzeRequirements(Path artifact, FabricModMetadata metadata, ReferenceInventory inventory,
            Set<BridgeCapability> required, List<Diagnostic> diagnostics, BridgeRequest request) {
        if (!metadata.mixins().isEmpty()) {
            required.add(BridgeCapability.MIXINS);
        }
        if (!inventory.mixinExtrasClasses().isEmpty()) {
            required.add(BridgeCapability.MIXIN_EXTRAS);
        }
        if (metadata.accessWidener().isPresent()) {
            required.add(BridgeCapability.ACCESS_WIDENERS);
            diagnostics.add(unsupported("LB-AW-001", metadata.id(), artifact,
                    "Access-widener transformation is not implemented in this scaffold build"));
        }
        if (!metadata.nestedJars().isEmpty()) {
            required.add(BridgeCapability.NESTED_JARS);
        }
        Set<String> adapters = new LinkedHashSet<>(metadata.languageAdapters().keySet());
        metadata.entrypoints().values().stream().flatMap(List::stream)
                .map(FabricEntrypoint::adapter).forEach(adapters::add);
        adapters.remove("default");
        adapters.remove("kotlin");
        if (!adapters.isEmpty()) {
            diagnostics.add(unsupported("LB-LANG-001", metadata.id(), artifact,
                    "Unsupported Fabric language adapters: " + adapters));
        }
        if (!inventory.fabricApiClasses().isEmpty()) {
            required.add(BridgeCapability.FABRIC_API);
            diagnostics.add(unsupported("LB-FAPI-001", metadata.id(), artifact,
                    "Unbridged Fabric API references: " + inventory.fabricApiClasses().stream().limit(5).toList()));
        }
        if (!inventory.loaderApiClasses().isEmpty()) {
            required.add(BridgeCapability.LOADER_API);
        }
        if (!inventory.nativeLibraries().isEmpty()) {
            diagnostics.add(unsupported("LB-NATIVE-001", metadata.id(), artifact,
                    "Native libraries require manual compatibility review: " + inventory.nativeLibraries()));
        }
        SourceNamespace namespace = sourceNamespace(request, inventory, diagnostics, metadata.id(), artifact);
        if (namespace == SourceNamespace.INTERMEDIARY) {
            required.add(BridgeCapability.REMAPPING);
        }
    }

    private static SourceNamespace sourceNamespace(BridgeRequest request, ReferenceInventory inventory,
            List<Diagnostic> diagnostics, String modId, Path artifact) {
        if (request.sourceNamespaceOverride().isPresent()) {
            String override = request.sourceNamespaceOverride().orElseThrow()
                    .toLowerCase(java.util.Locale.ROOT);
            if (override.equals("intermediary")) {
                return SourceNamespace.INTERMEDIARY;
            }
            if (override.equals("official") || override.equals("named")) {
                return SourceNamespace.NAMED;
            }
            if (diagnostics != null) {
                diagnostics.add(unsupported("LB-REMAP-002", modId, artifact,
                        "Unknown source namespace override: " + override));
            }
            return SourceNamespace.INVALID;
        }
        if (inventory.minecraftClasses().isEmpty()) {
            return SourceNamespace.NEUTRAL;
        }
        long intermediaryCount = inventory.minecraftClasses().stream()
                .map(name -> name.substring(name.lastIndexOf('.') + 1))
                .filter(name -> name.matches("class_[0-9]+(?:\\$.*)?"))
                .count();
        if (intermediaryCount == inventory.minecraftClasses().size()) {
            return SourceNamespace.INTERMEDIARY;
        }
        if (intermediaryCount == 0) {
            return SourceNamespace.NAMED;
        }
        if (diagnostics != null) {
            diagnostics.add(unsupported("LB-REMAP-003", modId, artifact,
                    "Mixed intermediary and named Minecraft references require --source-namespace"));
        }
        return SourceNamespace.INVALID;
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

    private static void diagnoseDuplicateIds(List<FabricModMetadata> metadata,
            List<Diagnostic> diagnostics) {
        Map<String, String> versions = new LinkedHashMap<>();
        for (FabricModMetadata mod : metadata) {
            String prior = versions.putIfAbsent(mod.id(), mod.version());
            if (prior != null && !prior.equals(mod.version())) {
                diagnostics.add(unsupported("LB-NESTED-006", mod.id(), null,
                        "Duplicate Fabric mod ID has conflicting versions " + prior
                                + " and " + mod.version()));
            }
        }
    }

    private void collectPreparationInputs(Path artifact, String source, String parentModId,
            String parentSubLocation, Path cacheDirectory,
            List<PreparationInput> destination, Set<String> seenArtifacts,
            Map<String, String> seenModVersions) throws IOException {
        String hash = sha256(artifact);
        if (!seenArtifacts.add(hash)) {
            return;
        }
        FabricModMetadata metadata = inspector.inspect(artifact).root();
        String priorVersion = seenModVersions.putIfAbsent(metadata.id(), metadata.version());
        if (priorVersion != null) {
            if (priorVersion.equals(metadata.version())) {
                return;
            }
            throw new IOException("LB-NESTED-006: duplicate Fabric mod ID '" + metadata.id()
                    + "' has conflicting versions " + priorVersion + " and " + metadata.version());
        }
        destination.add(new PreparationInput(
                artifact, source, metadata, parentModId, parentSubLocation));
        if (metadata.nestedJars().isEmpty()) {
            return;
        }
        Path nestedDirectory = cacheDirectory.resolve("nested-inputs");
        Files.createDirectories(nestedDirectory);
        try (ZipFile archive = new ZipFile(artifact.toFile())) {
            for (int index = 0; index < metadata.nestedJars().size(); index++) {
                String nestedLocation = metadata.nestedJars().get(index);
                var entry = archive.getEntry(nestedLocation);
                if (entry == null || entry.isDirectory()) {
                    throw new IOException("Declared nested JAR is missing: " + nestedLocation);
                }
                byte[] bytes;
                long maxBytes = JarReadLimits.DEFAULT.maxEntryBytes();
                if (entry.getSize() > maxBytes) {
                    throw new IOException("Nested JAR exceeds entry limit: " + nestedLocation);
                }
                try (var input = archive.getInputStream(entry)) {
                    bytes = input.readNBytes(Math.toIntExact(maxBytes + 1));
                }
                if (bytes.length > maxBytes) {
                    throw new IOException("Nested JAR exceeds entry limit: " + nestedLocation);
                }
                String nestedHash = sha256(bytes);
                Path nestedArtifact = nestedDirectory.resolve(nestedHash + ".jar");
                if (!Files.exists(nestedArtifact)) {
                    Files.write(nestedArtifact, bytes);
                }
                collectPreparationInputs(nestedArtifact, source + "!/" + nestedLocation,
                        metadata.id(), nestedLocation,
                        cacheDirectory, destination, seenArtifacts, seenModVersions);
            }
        }
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

    private static String implementationFingerprint() throws IOException {
        try {
            var source = FabricToForgeAdapter.class.getProtectionDomain().getCodeSource();
            if (source == null) return FabricAdapterVersion.CURRENT;
            Path location = Path.of(source.getLocation().toURI());
            if (Files.isRegularFile(location)) return sha256(location);
            if (!Files.isDirectory(location)) return FabricAdapterVersion.CURRENT;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var files = Files.walk(location)) {
                for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                    digest.update(location.relativize(file).toString().getBytes(StandardCharsets.UTF_8));
                    digest.update(Files.readAllBytes(file));
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.net.URISyntaxException | NoSuchAlgorithmException exception) {
            throw new IOException("Could not fingerprint the Fabric adapter implementation", exception);
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

    private static void writeLock(BridgeRequest request, List<PreparedArtifact> prepared,
            ResolvedMinecraftArtifacts minecraft, Path intermediaryMappings,
            String adapterFingerprint) throws IOException {
        Path lock = request.outputDirectory().resolve("bridge.lock.json");
        LockData data = new LockData("1", request.minecraftVersion(), "forge", request.hostVersion(),
                "fabric-to-forge", FabricAdapterVersion.CURRENT, adapterFingerprint, minecraft,
                intermediaryMappings == null ? null : sha256(intermediaryMappings), prepared);
        Files.writeString(lock, JSON.toJson(data), StandardCharsets.UTF_8);
    }

    private record PreparedArtifact(String modId, String source, String sourceSha256, String output,
            String outputSha256, String cacheKey, String sourceNamespace) {}

    private record PreparationInput(
            Path path,
            String source,
            FabricModMetadata metadata,
            String parentModId,
            String parentSubLocation) {}

    private record CompatibilityReport(String formatVersion, AdapterDescriptor adapter,
            List<ModInspection> mods, List<BridgeCapability> requiredCapabilities,
            List<Diagnostic> diagnostics, List<PreparedArtifact> preparedArtifacts) {}

    private record LockData(String formatVersion, String minecraftVersion, String hostLoader,
            String hostVersion, String adapter, String adapterVersion, String adapterArtifactSha256,
            ResolvedMinecraftArtifacts minecraftArtifacts, String intermediaryMappingsSha256,
            List<PreparedArtifact> artifacts) {}

    private enum SourceNamespace {
        NEUTRAL,
        INTERMEDIARY,
        NAMED,
        INVALID
    }
}
