package dev.loaderbridge.fabric.remap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.loaderbridge.api.AdapterDescriptor;
import dev.loaderbridge.api.BridgeAdapter;
import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.BridgeEnvironment;
import dev.loaderbridge.api.BridgePhase;
import dev.loaderbridge.api.BridgePlan;
import dev.loaderbridge.api.BridgeRequest;
import dev.loaderbridge.api.Diagnostic;
import dev.loaderbridge.api.DiagnosticSeverity;
import dev.loaderbridge.api.LoaderId;
import dev.loaderbridge.api.ModInspection;
import dev.loaderbridge.api.PreparationResult;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import dev.loaderbridge.fabric.metadata.FabricCandidateSelector;
import dev.loaderbridge.fabric.metadata.FabricDependencyResolver;
import dev.loaderbridge.fabric.metadata.FabricEntrypoint;
import dev.loaderbridge.fabric.metadata.FabricModInspector;
import dev.loaderbridge.fabric.metadata.FabricModMetadata;
import dev.loaderbridge.fabric.metadata.FabricLoaderCompatibility;
import dev.loaderbridge.fabric.metadata.FabricModTree;
import dev.loaderbridge.fabric.metadata.FabricVersionPredicate;
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
import java.util.ServiceLoader;
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
    private final List<RuntimeBridgeModuleProvider> bridgeModules;

    public FabricToForgeAdapter() {
        this(new MinecraftArtifactResolver(), new BundledIntermediaryMappings(),
                new MixinExtrasRuntimeResolver(), discoverBridgeModules());
    }

    FabricToForgeAdapter(MinecraftArtifactsProvider minecraftArtifacts,
            IntermediaryMappingsProvider intermediaryMappings) {
        this(minecraftArtifacts, intermediaryMappings, new MixinExtrasRuntimeResolver());
    }

    FabricToForgeAdapter(MinecraftArtifactsProvider minecraftArtifacts,
            IntermediaryMappingsProvider intermediaryMappings,
            RuntimeLibraryProvider mixinExtrasRuntime) {
        this(minecraftArtifacts, intermediaryMappings, mixinExtrasRuntime, discoverBridgeModules());
    }

    FabricToForgeAdapter(MinecraftArtifactsProvider minecraftArtifacts,
            IntermediaryMappingsProvider intermediaryMappings,
            RuntimeLibraryProvider mixinExtrasRuntime,
            List<RuntimeBridgeModuleProvider> bridgeModules) {
        this.minecraftArtifacts = java.util.Objects.requireNonNull(minecraftArtifacts, "minecraftArtifacts");
        this.intermediaryMappings = java.util.Objects.requireNonNull(intermediaryMappings, "intermediaryMappings");
        this.mixinExtrasRuntime = java.util.Objects.requireNonNull(mixinExtrasRuntime, "mixinExtrasRuntime");
        this.bridgeModules = validateBridgeModules(bridgeModules.stream()
                .sorted(java.util.Comparator.comparing(provider -> provider.descriptor().id()))
                .toList());
    }

    @Override
    public AdapterDescriptor descriptor() {
        return new AdapterDescriptor("fabric-to-forge", "1", FABRIC, FORGE, "=1.21.1", "[52.1.0,53)",
                List.of(BridgeCapability.METADATA, BridgeCapability.DEPENDENCY_RESOLUTION,
                        BridgeCapability.REMAPPING, BridgeCapability.MIXINS,
                        BridgeCapability.MIXIN_EXTRAS, BridgeCapability.ACCESS_WIDENERS));
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
        List<PreparationInput> analysisInputs = new ArrayList<>();
        Map<String, Integer> seenAnalysisArtifacts = new LinkedHashMap<>();
        Map<String, RuntimeBridgeModuleProvider> plannedBridgeModules = new LinkedHashMap<>();
        EnumSet<BridgeCapability> required = EnumSet.of(BridgeCapability.METADATA,
                BridgeCapability.DEPENDENCY_RESOLUTION);

        validateTarget(request, diagnostics);
        for (Path artifact : request.inputArtifacts()) {
            try {
                FabricModTree tree = inspector.inspect(artifact);
                FabricModMetadata metadata = tree.root();
                inspections.add(inspection(artifact, metadata, List.of()));
                if (!loadsInEnvironment(metadata, request.environment())) {
                    diagnostics.add(environmentSkipped(metadata, artifact, request.environment()));
                    continue;
                }
                diagnoseCompatibleMetadata(tree, request.environment(), diagnostics, artifact);
                collectPreparationInputs(artifact, artifact, artifact.toString(), null, null,
                        null, 0,
                        request.environment(), request.cacheDirectory(), analysisInputs,
                        seenAnalysisArtifacts);
            } catch (IOException exception) {
                diagnostics.add(error("LB-INSPECT-001", BridgePhase.INSPECT, null, artifact,
                        "Could not inspect Fabric mod", exception));
            }
        }
        Map<String, String> candidateVersions = candidateVersions(request);
        var candidateSelection = selectPreparationInputCandidates(
                analysisInputs, candidateVersions);
        diagnoseCandidateSelection(candidateSelection, analysisInputs, diagnostics);
        List<FabricModMetadata> allMetadata = candidateSelection.selected().stream()
                .map(PreparationInput::metadata).toList();
        diagnoseLanguageAdapterCollisions(allMetadata, diagnostics);
        diagnoseMixinConfigCollisions(allMetadata, request.environment(), diagnostics);
        if (candidateSelection.solved()) {
            for (PreparationInput input : candidateSelection.selected()) {
                if (isReplacedFabricApiNestedInput(input)) continue;
                try {
                    ReferenceInventory inventory = analyzer.analyze(input.path(),
                            nonRuntimeEntrypointClasses(input.metadata()));
                    analyzeRequirements(input.rootArtifact(), input.metadata(), inventory, required,
                            diagnostics, request, plannedBridgeModules);
                } catch (IOException exception) {
                    diagnostics.add(error("LB-INSPECT-001", BridgePhase.INSPECT,
                            input.metadata().id(), input.rootArtifact(),
                            "Could not analyze Fabric mod bytecode from " + input.source(),
                            exception));
                }
            }
        }
        Map<String, String> builtinVersions = new LinkedHashMap<>(Map.of(
                "minecraft", request.minecraftVersion(),
                "java", System.getProperty("java.specification.version").replaceFirst("^1\\.", ""),
                "fabricloader", FabricLoaderCompatibility.VERSION));
        plannedBridgeModules.values().forEach(provider ->
                builtinVersions.putAll(provider.descriptor().providedModVersions()));
        diagnostics.addAll(new FabricDependencyResolver().resolve(null, allMetadata, builtinVersions,
                Set.of("minecraft", "java", "fabricloader")));
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
        Map<String, Integer> seenArtifacts = new LinkedHashMap<>();
        String adapterFingerprint = implementationFingerprint();
        boolean needsMixinExtras = false;
        Map<String, RuntimeBridgeModuleProvider> selectedBridgeModules = new LinkedHashMap<>();
        for (Path source : request.inputArtifacts()) {
            collectPreparationInputs(source, source, source.toString(), null, null,
                    null, 0,
                    request.environment(), request.cacheDirectory(), inputs,
                    seenArtifacts);
        }
        inputs = selectPreparationInputs(inputs, candidateVersions(request));
        Map<String, String> dependencyOwners = dependencyOwners(inputs);
        for (PreparationInput input : inputs) {
            if (isReplacedFabricApiNestedInput(input)) {
                continue;
            }
            Path source = input.path();
            FabricModMetadata metadata = input.metadata();
            ReferenceInventory inventory = analyzer.analyze(source,
                    nonRuntimeEntrypointClasses(metadata));
            needsMixinExtras |= !inventory.mixinExtrasClasses().isEmpty();
            List<RuntimeBridgeModuleProvider> inputBridgeModules = modulesFor(
                    inventory.fabricApiClasses(), metadata);
            Map<String, String> fulfilledFabricDependencies = new java.util.TreeMap<>();
            for (RuntimeBridgeModuleProvider provider : inputBridgeModules) {
                selectedBridgeModules.put(provider.descriptor().id(), provider);
                fulfilledFabricDependencies.putAll(provider.descriptor().providedModVersions());
            }
            Map<String, String> resolvedDependencyModIds = resolvedDependencyModIds(
                    metadata, dependencyOwners);
            SourceNamespace namespace = sourceNamespace(request, inventory, null, metadata.id(), source);
            String sourceHash = sha256(source);
            Path preparationInput = source;
            Path runtimeMappings = null;
            String mappingKey = "namespace-neutral";
            boolean needsMappingResolver = inventory.loaderApiClasses().contains(
                    "net.fabricmc.loader.api.MappingResolver") || metadata.accessWidener().isPresent();
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
                    + request.hostVersion() + "|" + mappingKey + "|" + containmentKey + "|"
                    + fulfilledFabricDependencies + "|" + resolvedDependencyModIds)
                    .getBytes(StandardCharsets.UTF_8));
            Path cached = request.cacheDirectory().resolve(cacheKey + ".jar");
            if (!Files.exists(cached)) {
                PreparationManifest manifest = PreparationManifest.pinned(
                        request.minecraftVersion(), request.hostVersion())
                        .namespaces(namespace.name().toLowerCase(java.util.Locale.ROOT), "official")
                        .fulfilledFabricDependencies(fulfilledFabricDependencies)
                        .resolvedDependencyModIds(resolvedDependencyModIds);
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
        for (RuntimeBridgeModuleProvider provider : selectedBridgeModules.values()) {
            Path module = provider.artifact();
            if (!Files.isRegularFile(module, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("LB-MODULE-002: missing bridge module artifact: " + module);
            }
            var descriptor = provider.descriptor();
            String moduleHash = sha256(module);
            Path output = request.outputDirectory().resolve(
                    safe(descriptor.id()) + "-" + safe(descriptor.implementationVersion()) + ".jar");
            Files.copy(module, output, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            outputs.add(output);
            prepared.add(new PreparedArtifact(descriptor.id(), module.toString(), moduleHash,
                    output.toString(), sha256(output), moduleHash, "runtime-bridge-module"));
        }
        removeStaleManagedArtifacts(request.outputDirectory(), outputs);
        Path report = writeReport(request, plan, prepared);
        writeLock(request, prepared, resolvedMinecraft, resolvedIntermediaryMappings,
                adapterFingerprint);
        return new PreparationResult(outputs, report, plan.diagnostics());
    }

    private void analyzeRequirements(Path artifact, FabricModMetadata metadata, ReferenceInventory inventory,
            Set<BridgeCapability> required, List<Diagnostic> diagnostics, BridgeRequest request,
            Map<String, RuntimeBridgeModuleProvider> selectedBridgeModules) {
        if (!metadata.mixins().isEmpty()) {
            required.add(BridgeCapability.MIXINS);
        }
        if (!inventory.mixinSemanticFeatures().isEmpty()
                && !requiresModernFabricMixinLocals(metadata)) {
            diagnostics.add(unsupported("LB-MIXIN-017", metadata.id(), artifact,
                    "Mixin features " + inventory.mixinSemanticFeatures()
                            + " may require pre-0.12 Fabric local-variable semantics; Forge's"
                            + " stock Mixin 0.8.7 implements the modern algorithm. Declare a"
                            + " fabricloader lower bound of at least 0.12.0 or remove the legacy"
                            + " local-capture dependency."));
        }
        if (!inventory.mixinExtrasClasses().isEmpty()) {
            required.add(BridgeCapability.MIXIN_EXTRAS);
        }
        if (metadata.accessWidener().isPresent()) {
            required.add(BridgeCapability.ACCESS_WIDENERS);
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
        List<RuntimeBridgeModuleProvider> modules = modulesFor(inventory.fabricApiClasses(), metadata);
        modules.forEach(provider -> selectedBridgeModules.put(provider.descriptor().id(), provider));
        if (!modules.isEmpty()) {
            required.add(BridgeCapability.FABRIC_API);
        }
        if (inventory.fabricApiClasses().isEmpty() && !modules.isEmpty()) {
            diagnostics.add(info("LB-FAPI-100", metadata.id(), artifact,
                    "Automatically selected Fabric API bridges from dependencies: "
                            + moduleVersions(modules)));
        } else if (!inventory.fabricApiClasses().isEmpty()) {
            Set<String> uncovered = new LinkedHashSet<>(inventory.fabricApiClasses());
            modules.forEach(provider -> uncovered.removeAll(provider.descriptor().providedClasses()));
            if (uncovered.isEmpty()) {
                diagnostics.add(info("LB-FAPI-100", metadata.id(), artifact,
                        "Automatically selected Fabric API bridges: " + moduleVersions(modules)));
            } else {
                String references = "Unbridged Fabric API references: "
                        + uncovered.stream().limit(5).toList();
                boolean declared = metadata.dependencies().depends().keySet().stream()
                        .anyMatch(FabricToForgeAdapter::isFabricApiDependency);
                diagnostics.add(declared
                        ? unsupported("LB-FAPI-001", metadata.id(), artifact,
                                "Unbridged required " + references)
                        : warning("LB-FAPI-002", metadata.id(), artifact,
                                "Undeclared, potentially optional " + references));
            }
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

    private static Set<String> nonRuntimeEntrypointClasses(FabricModMetadata metadata) {
        Set<String> excluded = new LinkedHashSet<>();
        metadata.entrypoints().getOrDefault("fabric-datagen", List.of()).stream()
                .map(FabricEntrypoint::value)
                .map(value -> value.contains("::")
                        ? value.substring(0, value.indexOf("::")) : value)
                .forEach(value -> {
                    excluded.add(value);
                    int separator = value.lastIndexOf('.');
                    if (separator > 0
                            && value.substring(0, separator).endsWith(".datagen")) {
                        excluded.add(value.substring(0, separator) + ".*");
                    }
                });
        return Set.copyOf(excluded);
    }

    private static boolean requiresModernFabricMixinLocals(FabricModMetadata metadata) {
        List<String> predicates = metadata.dependencies().depends().get("fabricloader");
        if (predicates == null) {
            predicates = metadata.dependencies().depends().get("fabric-loader");
        }
        return predicates != null
                && FabricVersionPredicate.allAlternativesAtLeast(predicates, "0.12.0");
    }

    private static Map<String, String> dependencyOwners(List<PreparationInput> inputs) {
        Map<String, String> owners = new java.util.TreeMap<>();
        for (PreparationInput input : inputs) {
            owners.put(input.metadata().id(), input.metadata().id());
        }
        for (PreparationInput input : inputs) {
            String canonicalId = input.metadata().id();
            input.metadata().provides().forEach(alias -> owners.putIfAbsent(alias, canonicalId));
        }
        return Map.copyOf(owners);
    }

    private static Map<String, String> resolvedDependencyModIds(
            FabricModMetadata metadata, Map<String, String> dependencyOwners) {
        Set<String> dependencyIds = new java.util.TreeSet<>();
        dependencyIds.addAll(metadata.dependencies().depends().keySet());
        dependencyIds.addAll(metadata.dependencies().recommends().keySet());
        dependencyIds.addAll(metadata.dependencies().suggests().keySet());
        Map<String, String> resolved = new java.util.TreeMap<>();
        for (String dependencyId : dependencyIds) {
            String owner = dependencyOwners.get(dependencyId);
            if (owner != null && !owner.equals(dependencyId)) {
                resolved.put(dependencyId, owner);
            }
        }
        return Map.copyOf(resolved);
    }

    private List<RuntimeBridgeModuleProvider> modulesFor(Set<String> references,
            FabricModMetadata metadata) {
        Set<String> dependencies = metadata.dependencies().depends().keySet();
        Set<String> selectedIds = bridgeModules.stream().filter(provider ->
                provider.descriptor().providedClasses().stream().anyMatch(references::contains)
                        || provider.descriptor().providedModVersions().keySet().stream()
                                .anyMatch(dependencies::contains))
                .map(provider -> provider.descriptor().id())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        boolean changed;
        do {
            changed = false;
            for (RuntimeBridgeModuleProvider provider : bridgeModules) {
                if (selectedIds.contains(provider.descriptor().id())) {
                    changed |= selectedIds.addAll(provider.descriptor().requiredModules());
                }
            }
        } while (changed);
        return bridgeModules.stream()
                .filter(provider -> selectedIds.contains(provider.descriptor().id())).toList();
    }

    private static List<String> moduleVersions(List<RuntimeBridgeModuleProvider> modules) {
        return modules.stream().map(provider -> provider.descriptor().id() + "@"
                + provider.descriptor().implementationVersion()).toList();
    }

    private static List<RuntimeBridgeModuleProvider> discoverBridgeModules() {
        return ServiceLoader.load(RuntimeBridgeModuleProvider.class).stream()
                .map(ServiceLoader.Provider::get).toList();
    }

    private static List<RuntimeBridgeModuleProvider> validateBridgeModules(
            List<RuntimeBridgeModuleProvider> providers) {
        Map<String, String> owners = new LinkedHashMap<>();
        Set<String> moduleIds = providers.stream().map(provider -> provider.descriptor().id())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        for (RuntimeBridgeModuleProvider provider : providers) {
            var descriptor = provider.descriptor();
            claimModuleKey(owners, "module:" + descriptor.id(), descriptor.id());
            descriptor.providedClasses().forEach(className ->
                    claimModuleKey(owners, "class:" + className, descriptor.id()));
            descriptor.providedModVersions().keySet().forEach(modId ->
                    claimModuleKey(owners, "mod:" + modId, descriptor.id()));
            for (String required : descriptor.requiredModules()) {
                if (!moduleIds.contains(required)) {
                    throw new IllegalStateException("LB-MODULE-003: bridge module "
                            + descriptor.id() + " requires unavailable module " + required);
                }
            }
        }
        return List.copyOf(providers);
    }

    private static void claimModuleKey(Map<String, String> owners, String key, String moduleId) {
        String previous = owners.putIfAbsent(key, moduleId);
        if (previous != null) {
            throw new IllegalStateException("LB-MODULE-001: bridge modules " + previous + " and "
                    + moduleId + " both claim " + key.substring(key.indexOf(':') + 1));
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
        long total = inventory.minecraftClasses().size();
        if (intermediaryCount == total
                || (total >= 10 && intermediaryCount / (double) total >= 0.95d)) {
            return SourceNamespace.INTERMEDIARY;
        }
        if (intermediaryCount == 0
                || (total >= 10 && (total - intermediaryCount) / (double) total >= 0.95d)) {
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

    private static void diagnoseCompatibleMetadata(
            FabricModTree tree, BridgeEnvironment environment,
            List<Diagnostic> diagnostics, Path artifact) {
        FabricModMetadata metadata = tree.root();
        if (!loadsInEnvironment(metadata, environment)) {
            diagnostics.add(environmentSkipped(metadata, artifact, environment));
            return;
        }
        tree.nested().forEach(child -> diagnoseCompatibleMetadata(
                child, environment, diagnostics, artifact));
    }

    private static boolean loadsInEnvironment(
            FabricModMetadata metadata, BridgeEnvironment environment) {
        return metadata.environment().equals("*")
                || metadata.environment().equals(
                        environment.name().toLowerCase(java.util.Locale.ROOT));
    }

    private static void diagnoseLanguageAdapterCollisions(
            List<FabricModMetadata> metadata, List<Diagnostic> diagnostics) {
        Map<String, String> owners = new LinkedHashMap<>();
        owners.put("default", "fabricloader");
        for (FabricModMetadata mod : metadata) {
            for (String key : mod.languageAdapters().keySet()) {
                String prior = owners.putIfAbsent(key, mod.id());
                if (prior != null) {
                    diagnostics.add(unsupported("LB-LANG-002", mod.id(), null,
                            "Fabric language adapter key '" + key
                                    + "' is already defined by " + prior));
                }
            }
        }
    }

    private static void diagnoseMixinConfigCollisions(List<FabricModMetadata> metadata,
            BridgeEnvironment environment, List<Diagnostic> diagnostics) {
        String side = environment.name().toLowerCase(java.util.Locale.ROOT);
        Map<String, String> owners = new LinkedHashMap<>();
        for (FabricModMetadata mod : metadata) {
            mod.mixins().stream()
                    .filter(mixin -> mixin.environment().equals("*")
                            || mixin.environment().equals(side))
                    .forEach(mixin -> {
                        String prior = owners.putIfAbsent(mixin.config(), mod.id());
                        if (prior != null) {
                            diagnostics.add(unsupported("LB-MIXIN-002", mod.id(), null,
                                    "Fabric Mixin config '" + mixin.config()
                                            + "' is already declared by " + prior));
                        }
                    });
        }
    }

    private Map<String, String> candidateVersions(BridgeRequest request) {
        Map<String, String> versions = new LinkedHashMap<>();
        versions.put("minecraft", request.minecraftVersion());
        versions.put("java", System.getProperty("java.specification.version")
                .replaceFirst("^1\\.", ""));
        versions.put("fabricloader", FabricLoaderCompatibility.VERSION);
        bridgeModules.forEach(provider ->
                versions.putAll(provider.descriptor().providedModVersions()));
        return Map.copyOf(versions);
    }

    private static Diagnostic environmentSkipped(
            FabricModMetadata metadata, Path artifact, BridgeEnvironment environment) {
        return info("LB-ENV-100", metadata.id(), artifact,
                "Skipped " + metadata.environment() + "-only Fabric mod for "
                        + environment.name().toLowerCase(java.util.Locale.ROOT) + " preparation");
    }

    private static FabricCandidateSelector.Result<PreparationInput>
            selectPreparationInputCandidates(List<PreparationInput> inputs,
                    Map<String, String> availableVersions) {
        var selection = FabricCandidateSelector.select(inputs.stream()
                .map(input -> new FabricCandidateSelector.Candidate<>(input, input.metadata(),
                        input.parentModId() == null, input.candidateKey(),
                        input.parentLinks().keySet(), input.depth()))
                .toList(), availableVersions);
        if (!selection.solved()) return selection;
        Set<String> selectedKeys = selection.selected().stream()
                .map(PreparationInput::candidateKey).collect(
                        java.util.stream.Collectors.toUnmodifiableSet());
        List<PreparationInput> normalized = selection.selected().stream()
                .map(input -> input.withSelectedParent(selectedKeys)).toList();
        return new FabricCandidateSelector.Result<>(
                normalized, selection.status(), selection.detail());
    }

    private static void diagnoseCandidateSelection(
            FabricCandidateSelector.Result<PreparationInput> selection,
            List<PreparationInput> candidates, List<Diagnostic> diagnostics) {
        switch (selection.status()) {
            case DUPLICATE_ROOTS -> diagnostics.add(unsupported(
                    "LB-NESTED-006", null, null, selection.detail()));
            case UNSATISFIABLE -> diagnostics.add(unsupported(
                    "LB-NESTED-007", null, null, selection.detail()));
            case BUDGET_EXCEEDED -> diagnostics.add(unsupported(
                    "LB-NESTED-008", null, null, selection.detail()));
            case SOLVED -> { }
        }
        Map<String, Long> candidateCounts = candidates.stream().collect(
                java.util.stream.Collectors.groupingBy(candidate -> candidate.metadata().id(),
                        LinkedHashMap::new, java.util.stream.Collectors.counting()));
        for (PreparationInput candidate : selection.selected()) {
            long count = candidateCounts.getOrDefault(candidate.metadata().id(), 0L);
            if (count > 1 && candidate.parentModId() != null) {
                diagnostics.add(info("LB-NESTED-101", candidate.metadata().id(), null,
                        "Selected Fabric candidate " + candidate.metadata().id() + " "
                                + candidate.metadata().version() + " from " + count
                                + " available nested variants"));
            }
        }
    }

    private static List<PreparationInput> selectPreparationInputs(List<PreparationInput> inputs,
            Map<String, String> availableVersions) throws IOException {
        var selection = selectPreparationInputCandidates(inputs, availableVersions);
        if (!selection.solved()) {
            String code = switch (selection.status()) {
                case DUPLICATE_ROOTS -> "LB-NESTED-006";
                case UNSATISFIABLE -> "LB-NESTED-007";
                case BUDGET_EXCEEDED -> "LB-NESTED-008";
                case SOLVED -> throw new IllegalStateException("unreachable");
            };
            throw new IOException(code + ": " + selection.detail());
        }
        return selection.selected();
    }

    private void collectPreparationInputs(Path artifact, Path rootArtifact, String source,
            String parentModId, String parentSubLocation, String parentCandidateKey, int depth,
            BridgeEnvironment environment, Path cacheDirectory,
            List<PreparationInput> destination, Map<String, Integer> seenArtifacts) throws IOException {
        String hash = sha256(artifact);
        Integer previousIndex = seenArtifacts.get(hash);
        if (previousIndex != null) {
            PreparationInput previous = destination.get(previousIndex);
            Map<String, ParentLink> parentLinks = new LinkedHashMap<>(previous.parentLinks());
            if (parentCandidateKey != null) {
                parentLinks.putIfAbsent(parentCandidateKey,
                        new ParentLink(parentModId, parentSubLocation));
            }
            if (parentModId == null && previous.parentModId() != null) {
                destination.set(previousIndex, new PreparationInput(
                        artifact, artifact, source, previous.metadata(), null, null,
                        hash, parentLinks, 0));
            } else if (!parentLinks.equals(previous.parentLinks())
                    || depth > previous.depth()) {
                destination.set(previousIndex, new PreparationInput(
                        previous.path(), previous.rootArtifact(), previous.source(),
                        previous.metadata(), previous.parentModId(), previous.parentSubLocation(),
                        hash, parentLinks, Math.max(depth, previous.depth())));
            }
            return;
        }
        FabricModMetadata metadata = inspector.inspect(artifact).root();
        if (!loadsInEnvironment(metadata, environment)) {
            return;
        }
        seenArtifacts.put(hash, destination.size());
        Map<String, ParentLink> parentLinks = parentCandidateKey == null
                ? Map.of() : Map.of(parentCandidateKey,
                        new ParentLink(parentModId, parentSubLocation));
        destination.add(new PreparationInput(
                artifact, rootArtifact, source, metadata, parentModId, parentSubLocation,
                hash, parentLinks, depth));
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
                collectPreparationInputs(nestedArtifact, rootArtifact,
                        source + "!/" + nestedLocation,
                        metadata.id(), nestedLocation, hash, depth + 1, environment,
                        cacheDirectory, destination, seenArtifacts);
            }
        }
    }

    private static Diagnostic unsupported(String code, String modId, Path artifact, String message) {
        return new Diagnostic(DiagnosticSeverity.ERROR, code, BridgePhase.PLAN, modId, artifact, message, null);
    }

    private static Diagnostic warning(String code, String modId, Path artifact, String message) {
        return new Diagnostic(DiagnosticSeverity.WARNING, code, BridgePhase.PLAN,
                modId, artifact, message, null);
    }

    private static Diagnostic info(String code, String modId, Path artifact, String message) {
        return new Diagnostic(DiagnosticSeverity.INFO, code, BridgePhase.PLAN, modId,
                artifact, message, null);
    }

    private static boolean isFabricApiDependency(String id) {
        return id.equals("fabric-api") || (id.startsWith("fabric-") && id.contains("api"));
    }

    private static boolean isReplacedFabricApiNestedInput(PreparationInput input) {
        return ("fabric-api".equals(input.parentModId())
                && !"fabric-transitive-access-wideners-v1".equals(input.metadata().id()))
                || "fabric-data-generation-api-v1".equals(input.metadata().id());
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
                request.environment(),
                "fabric-to-forge", FabricAdapterVersion.CURRENT, adapterFingerprint, minecraft,
                intermediaryMappings == null ? null : sha256(intermediaryMappings), prepared);
        Files.writeString(lock, JSON.toJson(data), StandardCharsets.UTF_8);
    }

    private static void removeStaleManagedArtifacts(Path outputDirectory, List<Path> currentOutputs)
            throws IOException {
        Path lock = outputDirectory.resolve("bridge.lock.json");
        if (!Files.isRegularFile(lock)) return;

        Path managedDirectory = outputDirectory.toAbsolutePath().normalize();
        List<Path> previouslyManaged = new ArrayList<>();
        try {
            com.google.gson.JsonObject root = com.google.gson.JsonParser
                    .parseString(Files.readString(lock)).getAsJsonObject();
            if (!root.has("adapter") || !root.get("adapter").isJsonPrimitive()
                    || !"fabric-to-forge".equals(root.get("adapter").getAsString())
                    || !root.has("artifacts") || !root.get("artifacts").isJsonArray()) {
                return;
            }
            for (com.google.gson.JsonElement element : root.getAsJsonArray("artifacts")) {
                if (!element.isJsonObject() || !element.getAsJsonObject().has("output")
                        || !element.getAsJsonObject().get("output").isJsonPrimitive()) {
                    continue;
                }
                Path candidate = Path.of(element.getAsJsonObject().get("output").getAsString())
                        .toAbsolutePath().normalize();
                if (managedDirectory.equals(candidate.getParent())
                        && candidate.getFileName().toString().endsWith(".jar")) {
                    previouslyManaged.add(candidate);
                }
            }
        } catch (RuntimeException exception) {
            throw new IOException("LB-LOCK-003: existing bridge.lock.json is malformed", exception);
        }
        Set<Path> retained = currentOutputs.stream()
                .map(path -> path.toAbsolutePath().normalize())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        for (Path managed : previouslyManaged) {
            if (!retained.contains(managed)) Files.deleteIfExists(managed);
        }
    }

    private record PreparedArtifact(String modId, String source, String sourceSha256, String output,
            String outputSha256, String cacheKey, String sourceNamespace) {}

    private record PreparationInput(
            Path path,
            Path rootArtifact,
            String source,
            FabricModMetadata metadata,
            String parentModId,
            String parentSubLocation,
            String candidateKey,
            Map<String, ParentLink> parentLinks,
            int depth) {
        private PreparationInput {
            parentLinks = Map.copyOf(parentLinks);
        }

        private PreparationInput withSelectedParent(Set<String> selectedKeys) {
            if (parentModId == null) return this;
            ParentLink selected = parentLinks.entrySet().stream()
                    .filter(entry -> selectedKeys.contains(entry.getKey()))
                    .sorted(Map.Entry.comparingByKey())
                    .map(Map.Entry::getValue).findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "selected nested candidate has no selected parent"));
            if (selected.modId().equals(parentModId)
                    && selected.subLocation().equals(parentSubLocation)) return this;
            return new PreparationInput(path, rootArtifact, source, metadata,
                    selected.modId(), selected.subLocation(), candidateKey, parentLinks, depth);
        }
    }

    private record ParentLink(String modId, String subLocation) {}

    private record CompatibilityReport(String formatVersion, AdapterDescriptor adapter,
            List<ModInspection> mods, List<BridgeCapability> requiredCapabilities,
            List<Diagnostic> diagnostics, List<PreparedArtifact> preparedArtifacts) {}

    private record LockData(String formatVersion, String minecraftVersion, String hostLoader,
            String hostVersion, BridgeEnvironment environment, String adapter, String adapterVersion,
            String adapterArtifactSha256,
            ResolvedMinecraftArtifacts minecraftArtifacts, String intermediaryMappingsSha256,
            List<PreparedArtifact> artifacts) {}

    private enum SourceNamespace {
        NEUTRAL,
        INTERMEDIARY,
        NAMED,
        INVALID
    }
}
