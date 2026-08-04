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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.IdentityHashMap;
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
        List<MetadataCandidate> allCandidates = new ArrayList<>();
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
                collectCompatibleMetadata(tree, request.environment(), allCandidates,
                        diagnostics, artifact, true);
                ReferenceInventory inventory = analyzer.analyze(artifact,
                        nonRuntimeEntrypointClasses(metadata));
                analyzeRequirements(artifact, metadata, inventory, required, diagnostics, request,
                        plannedBridgeModules);
            } catch (IOException exception) {
                diagnostics.add(error("LB-INSPECT-001", BridgePhase.INSPECT, null, artifact,
                        "Could not inspect Fabric mod", exception));
            }
        }
        List<FabricModMetadata> allMetadata = selectMetadataCandidates(allCandidates, diagnostics)
                .stream().map(MetadataCandidate::metadata).toList();
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
            collectPreparationInputs(source, source.toString(), null, null,
                    request.environment(), request.cacheDirectory(), inputs,
                    seenArtifacts);
        }
        inputs = selectPreparationInputs(inputs);
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

    private static void collectCompatibleMetadata(
            FabricModTree tree,
            BridgeEnvironment environment,
            List<MetadataCandidate> destination,
            List<Diagnostic> diagnostics,
            Path artifact,
            boolean root) {
        FabricModMetadata metadata = tree.root();
        if (!loadsInEnvironment(metadata, environment)) {
            diagnostics.add(environmentSkipped(metadata, artifact, environment));
            return;
        }
        destination.add(new MetadataCandidate(metadata, root));
        tree.nested().forEach(child -> collectCompatibleMetadata(
                child, environment, destination, diagnostics, artifact, false));
    }

    private static boolean loadsInEnvironment(
            FabricModMetadata metadata, BridgeEnvironment environment) {
        return metadata.environment().equals("*")
                || metadata.environment().equals(
                        environment.name().toLowerCase(java.util.Locale.ROOT));
    }

    private static Diagnostic environmentSkipped(
            FabricModMetadata metadata, Path artifact, BridgeEnvironment environment) {
        return info("LB-ENV-100", metadata.id(), artifact,
                "Skipped " + metadata.environment() + "-only Fabric mod for "
                        + environment.name().toLowerCase(java.util.Locale.ROOT) + " preparation");
    }

    private static List<MetadataCandidate> selectMetadataCandidates(
            List<MetadataCandidate> candidates, List<Diagnostic> diagnostics) {
        Map<String, List<MetadataCandidate>> groups = new LinkedHashMap<>();
        candidates.forEach(candidate -> groups.computeIfAbsent(candidate.metadata().id(),
                ignored -> new ArrayList<>()).add(candidate));
        List<FabricModMetadata> fixed = candidates.stream()
                .filter(candidate -> candidate.root()
                        || groups.get(candidate.metadata().id()).size() == 1)
                .map(MetadataCandidate::metadata).toList();
        Map<String, MetadataCandidate> choices = new LinkedHashMap<>();
        for (Map.Entry<String, List<MetadataCandidate>> entry : groups.entrySet()) {
            List<MetadataCandidate> variants = entry.getValue();
            List<MetadataCandidate> roots = variants.stream().filter(MetadataCandidate::root).toList();
            if (roots.size() > 1) {
                FabricModMetadata mod = roots.getFirst().metadata();
                diagnostics.add(unsupported("LB-NESTED-006", mod.id(), null,
                        "Duplicate root Fabric mods claim ID '" + mod.id() + "'"));
                choices.put(entry.getKey(), roots.getFirst());
            } else {
                choices.put(entry.getKey(), roots.isEmpty()
                        ? bestCandidate(variants, Map.of()) : roots.getFirst());
            }
        }
        if (!stabilizeMetadataChoices(groups, fixed, choices)) {
            diagnostics.add(unsupported("LB-NESTED-007", null, null,
                    "Fabric nested candidate constraints did not converge"));
        }
        Set<MetadataCandidate> selected = Collections.newSetFromMap(new IdentityHashMap<>());
        selected.addAll(choices.values());
        for (List<MetadataCandidate> variants : groups.values()) {
            MetadataCandidate choice = choices.get(variants.getFirst().metadata().id());
            long rootCount = variants.stream().filter(MetadataCandidate::root).count();
            if (variants.size() > 1 && rootCount <= 1) {
                diagnostics.add(info("LB-NESTED-101", choice.metadata().id(), null,
                        "Selected Fabric candidate " + choice.metadata().id() + " "
                                + choice.metadata().version() + " from " + variants.size()
                                + " available nested variants"));
            }
        }
        return candidates.stream().filter(selected::contains).toList();
    }

    private static boolean stabilizeMetadataChoices(
            Map<String, List<MetadataCandidate>> groups,
            List<FabricModMetadata> fixed,
            Map<String, MetadataCandidate> choices) {
        for (int pass = 0; pass <= groups.size(); pass++) {
            List<FabricModMetadata> active = new ArrayList<>(fixed);
            choices.values().stream().map(MetadataCandidate::metadata).forEach(active::add);
            Map<String, List<List<String>>> constraints = requiredPredicates(active);
            boolean changed = false;
            for (Map.Entry<String, List<MetadataCandidate>> entry : groups.entrySet()) {
                if (entry.getValue().stream().anyMatch(MetadataCandidate::root)) continue;
                MetadataCandidate choice = bestCandidate(entry.getValue(), constraints);
                if (choices.put(entry.getKey(), choice) != choice) changed = true;
            }
            if (!changed) return true;
        }
        return false;
    }

    private static MetadataCandidate bestCandidate(List<MetadataCandidate> variants,
            Map<String, List<List<String>>> constraints) {
        List<MetadataCandidate> compatible = variants.stream()
                .filter(candidate -> matchesRequiredPredicates(candidate.metadata(), constraints))
                .toList();
        List<MetadataCandidate> choices = compatible.isEmpty() ? variants : compatible;
        MetadataCandidate best = choices.getFirst();
        for (int index = 1; index < choices.size(); index++) {
            MetadataCandidate candidate = choices.get(index);
            if (FabricVersionPredicate.compare(candidate.metadata().version(),
                    best.metadata().version()) > 0) best = candidate;
        }
        return best;
    }

    private static List<PreparationInput> selectPreparationInputs(List<PreparationInput> inputs)
            throws IOException {
        Map<String, List<PreparationInput>> groups = new LinkedHashMap<>();
        inputs.forEach(input -> groups.computeIfAbsent(input.metadata().id(),
                ignored -> new ArrayList<>()).add(input));
        List<FabricModMetadata> fixed = inputs.stream()
                .filter(input -> input.parentModId() == null
                        || groups.get(input.metadata().id()).size() == 1)
                .map(PreparationInput::metadata).toList();
        Map<String, PreparationInput> choices = new LinkedHashMap<>();
        for (Map.Entry<String, List<PreparationInput>> entry : groups.entrySet()) {
            List<PreparationInput> variants = entry.getValue();
            List<PreparationInput> roots = variants.stream()
                    .filter(input -> input.parentModId() == null).toList();
            if (roots.size() > 1) {
                throw new IOException("LB-NESTED-006: duplicate root Fabric mods claim ID '"
                        + roots.getFirst().metadata().id() + "'");
            }
            if (roots.size() == 1) {
                choices.put(entry.getKey(), roots.getFirst());
            } else {
                choices.put(entry.getKey(), bestPreparationInput(variants, Map.of()));
            }
        }
        if (!stabilizePreparationChoices(groups, fixed, choices)) {
            throw new IOException("LB-NESTED-007: Fabric nested candidate constraints did not converge");
        }
        Set<PreparationInput> selected = Collections.newSetFromMap(new IdentityHashMap<>());
        selected.addAll(choices.values());
        return inputs.stream().filter(selected::contains).toList();
    }

    private static boolean stabilizePreparationChoices(
            Map<String, List<PreparationInput>> groups,
            List<FabricModMetadata> fixed,
            Map<String, PreparationInput> choices) {
        for (int pass = 0; pass <= groups.size(); pass++) {
            List<FabricModMetadata> active = new ArrayList<>(fixed);
            choices.values().stream().map(PreparationInput::metadata).forEach(active::add);
            Map<String, List<List<String>>> constraints = requiredPredicates(active);
            boolean changed = false;
            for (Map.Entry<String, List<PreparationInput>> entry : groups.entrySet()) {
                if (entry.getValue().stream().anyMatch(input -> input.parentModId() == null)) continue;
                PreparationInput choice = bestPreparationInput(entry.getValue(), constraints);
                if (choices.put(entry.getKey(), choice) != choice) changed = true;
            }
            if (!changed) return true;
        }
        return false;
    }

    private static PreparationInput bestPreparationInput(List<PreparationInput> variants,
            Map<String, List<List<String>>> constraints) {
        List<PreparationInput> compatible = variants.stream()
                .filter(input -> matchesRequiredPredicates(input.metadata(), constraints)).toList();
        List<PreparationInput> choices = compatible.isEmpty() ? variants : compatible;
        PreparationInput best = choices.getFirst();
        for (int index = 1; index < choices.size(); index++) {
            PreparationInput candidate = choices.get(index);
            if (FabricVersionPredicate.compare(candidate.metadata().version(),
                    best.metadata().version()) > 0) best = candidate;
        }
        return best;
    }

    private static Map<String, List<List<String>>> requiredPredicates(
            List<FabricModMetadata> metadata) {
        Map<String, List<List<String>>> result = new LinkedHashMap<>();
        for (FabricModMetadata mod : metadata) {
            mod.dependencies().depends().forEach((id, predicates) ->
                    result.computeIfAbsent(id, ignored -> new ArrayList<>()).add(predicates));
        }
        return result;
    }

    private static boolean matchesRequiredPredicates(FabricModMetadata candidate,
            Map<String, List<List<String>>> constraints) {
        List<String> claims = new ArrayList<>(candidate.provides());
        claims.add(candidate.id());
        for (String claim : claims) {
            for (List<String> predicates : constraints.getOrDefault(claim, List.of())) {
                if (!FabricVersionPredicate.anyMatches(predicates, candidate.version())) return false;
            }
        }
        return true;
    }

    private void collectPreparationInputs(Path artifact, String source, String parentModId,
            String parentSubLocation, BridgeEnvironment environment, Path cacheDirectory,
            List<PreparationInput> destination, Map<String, Integer> seenArtifacts) throws IOException {
        String hash = sha256(artifact);
        Integer previousIndex = seenArtifacts.get(hash);
        if (previousIndex != null) {
            PreparationInput previous = destination.get(previousIndex);
            if (parentModId == null && previous.parentModId() != null) {
                destination.set(previousIndex, new PreparationInput(
                        artifact, source, previous.metadata(), null, null));
            }
            return;
        }
        FabricModMetadata metadata = inspector.inspect(artifact).root();
        if (!loadsInEnvironment(metadata, environment)) {
            return;
        }
        seenArtifacts.put(hash, destination.size());
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
                        metadata.id(), nestedLocation, environment,
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

    private record MetadataCandidate(FabricModMetadata metadata, boolean root) {}

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
