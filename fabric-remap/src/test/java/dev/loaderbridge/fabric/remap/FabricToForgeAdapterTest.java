package dev.loaderbridge.fabric.remap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.loaderbridge.api.BridgeAdapter;
import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.BridgeEnvironment;
import dev.loaderbridge.api.BridgeRequest;
import dev.loaderbridge.api.DiagnosticSeverity;
import dev.loaderbridge.api.LoaderId;
import dev.loaderbridge.api.RuntimeBridgeModule;
import dev.loaderbridge.api.RuntimeBridgeModuleProvider;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

class FabricToForgeAdapterTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void serviceLoaderFindsAdapterAndPreparesNamespaceNeutralMod() throws Exception {
        BridgeAdapter adapter = ServiceLoader.load(BridgeAdapter.class).findFirst().orElseThrow();
        Path source = temporaryDirectory.resolve("mod.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(source))) {
            jar.putNextEntry(new JarEntry("fabric.mod.json"));
            jar.write(("{\"schemaVersion\":1,\"id\":\"fixture\",\"version\":\"1.0.0\","
                    + "\"entrypoints\":{\"main\":[\"fixture.Main\"]}}").getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
        BridgeRequest request = new BridgeRequest("1.21.1", new LoaderId("forge"), "52.1.0",
                BridgeEnvironment.CLIENT, List.of(source), temporaryDirectory.resolve("output"),
                temporaryDirectory.resolve("cache"));

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(result.succeeded()).isTrue();
        assertThat(result.artifacts()).hasSize(1).allMatch(Files::exists);
        assertThat(Files.readString(result.report())).contains("fixture", "main");
        assertThat(Files.readString(request.outputDirectory().resolve("bridge.lock.json")))
                .contains("sourceSha256", "outputSha256", "\"adapterVersion\": \"0.4.0\"",
                        "adapterArtifactSha256");
        try (JarFile jar = new JarFile(result.artifacts().getFirst().toFile())) {
            assertThat(jar.getEntry("pack.mcmeta")).isNotNull();
            assertThat(new String(jar.getInputStream(jar.getJarEntry("META-INF/loaderbridge.json"))
                    .readAllBytes(), StandardCharsets.UTF_8))
                    .contains("\"sourceNamespace\": \"neutral\"");
        }
    }

    @Test
    void inventoriesImplementedMixinAccessWidenerAndNestedJarCapabilities() throws Exception {
        Path nested = temporaryDirectory.resolve("nested.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(nested))) {
            jar.putNextEntry(new JarEntry("fabric.mod.json"));
            jar.write("{\"schemaVersion\":1,\"id\":\"child\",\"version\":\"1\"}"
                    .getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
        Path source = temporaryDirectory.resolve("complex.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(source))) {
            jar.putNextEntry(new JarEntry("fabric.mod.json"));
            jar.write(("{\"schemaVersion\":1,\"id\":\"complex\",\"version\":\"1\","
                    + "\"mixins\":[\"complex.mixins.json\"],\"accessWidener\":\"complex.accesswidener\","
                    + "\"jars\":[{\"file\":\"META-INF/jars/child.jar\"}]}")
                    .getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("META-INF/jars/child.jar"));
            jar.write(Files.readAllBytes(nested));
            jar.closeEntry();
        }
        BridgeRequest request = new BridgeRequest("1.21.1", new LoaderId("forge"), "52.1.0",
                BridgeEnvironment.CLIENT, List.of(source), temporaryDirectory.resolve("output-complex"),
                temporaryDirectory.resolve("cache-complex"));

        var plan = new FabricToForgeAdapter().plan(request);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(plan.requiredCapabilities()).contains(
                BridgeCapability.MIXINS, BridgeCapability.ACCESS_WIDENERS,
                BridgeCapability.NESTED_JARS);
        assertThat(plan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .doesNotContain("LB-AW-001", "LB-MIXIN-001", "LB-NESTED-001");
    }

    @Test
    void automaticallyAddsPinnedMixinExtrasRuntimeWhenAnnotationsRequireIt() throws Exception {
        Path source = temporaryDirectory.resolve("mixinextras-mod.jar");
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "fixture/ExtrasMixin", null,
                "java/lang/Object", null);
        var method = writer.visitMethod(Opcodes.ACC_PRIVATE, "modify", "(Z)Z", null, null);
        method.visitAnnotation("Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;", false)
                .visitEnd();
        method.visitEnd();
        writer.visitEnd();
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(source))) {
            jar.putNextEntry(new JarEntry("fabric.mod.json"));
            jar.write("{\"schemaVersion\":1,\"id\":\"extras_fixture\",\"version\":\"1\"}"
                    .getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("fixture/ExtrasMixin.class"));
            jar.write(writer.toByteArray());
            jar.closeEntry();
        }
        Path runtime = temporaryDirectory.resolve("mixinextras-forge-test.jar");
        Files.writeString(runtime, "controlled-runtime", StandardCharsets.UTF_8);
        RuntimeLibraryProvider provider = (cache, refresh) -> new ResolvedRuntimeLibrary(
                "mixinextras-forge", "test", java.net.URI.create("https://example.invalid/runtime.jar"),
                "controlled-sha256", runtime);
        FabricToForgeAdapter adapter = new FabricToForgeAdapter(
                (version, cache, refresh) -> { throw new AssertionError("Minecraft not required"); },
                (version, cache) -> { throw new AssertionError("Mappings not required"); }, provider);
        BridgeRequest request = new BridgeRequest("1.21.1", new LoaderId("forge"), "52.1.0",
                BridgeEnvironment.SERVER, List.of(source), temporaryDirectory.resolve("extras-output"),
                temporaryDirectory.resolve("extras-cache"));

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.requiredCapabilities()).contains(BridgeCapability.MIXIN_EXTRAS);
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .containsExactlyInAnyOrder("extras_fixture-1-loaderbridge.jar",
                        "mixinextras-forge-test.jar");
        assertThat(Files.readString(request.outputDirectory().resolve("bridge.lock.json")))
                .contains("https://example.invalid/runtime.jar", "runtime-library");
    }

    @Test
    void acceptsAHighConfidenceDominantIntermediaryNamespace() throws Exception {
        Path source = referencedMod("dominant", null, writer -> {
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "references", "()V", null, null);
            for (int index = 1; index <= 20; index++) {
                method.visitTypeInsn(Opcodes.CHECKCAST, "net/minecraft/class_" + index);
            }
            method.visitTypeInsn(Opcodes.CHECKCAST, "net/minecraft/server/MinecraftServer");
            method.visitEnd();
        });
        BridgeRequest request = new BridgeRequest("1.21.1", new LoaderId("forge"), "52.1.0",
                BridgeEnvironment.SERVER, List.of(source), temporaryDirectory.resolve("dominant-output"),
                temporaryDirectory.resolve("dominant-cache"));

        var plan = new FabricToForgeAdapter().plan(request);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(plan.requiredCapabilities()).contains(BridgeCapability.REMAPPING);
        assertThat(plan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .doesNotContain("LB-REMAP-003");
    }

    @Test
    void treatsUndeclaredFabricApiReferencesAsOptionalButDeclaredOnesAsRequired()
            throws Exception {
        java.util.function.Consumer<ClassWriter> reference = writer -> {
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "references", "()V", null, null);
            method.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "net/fabricmc/fabric/api/transfer/v1/item/ItemStorage", "find", "()V", false);
            method.visitEnd();
        };
        Path optional = referencedMod("optional_api", null, reference);
        Path required = referencedMod("required_api", "fabric-api", reference);

        var optionalPlan = new FabricToForgeAdapter().plan(requestFor(optional, "optional-api"));
        var requiredPlan = new FabricToForgeAdapter().plan(requestFor(required, "required-api"));

        assertThat(optionalPlan.canPrepare()).isTrue();
        assertThat(optionalPlan.diagnostics()).anySatisfy(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo("LB-FAPI-002");
            assertThat(diagnostic.severity()).isEqualTo(DiagnosticSeverity.WARNING);
        });
        assertThat(requiredPlan.canPrepare()).isFalse();
        assertThat(requiredPlan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .contains("LB-FAPI-001");
    }

    @Test
    void automaticallySelectsAndInstallsFabricApiBaseBridge() throws Exception {
        Path source = referencedMod("event_api", "fabric-api-base", writer -> {
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "references", "()V", null, null);
            method.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "net/fabricmc/fabric/api/event/EventFactory", "createArrayBacked",
                    "(Ljava/lang/Class;Ljava/util/function/Function;)"
                            + "Lnet/fabricmc/fabric/api/event/Event;", false);
            method.visitInsn(Opcodes.POP);
            method.visitInsn(Opcodes.RETURN);
            method.visitMaxs(2, 1);
            method.visitEnd();
        });
        BridgeRequest request = requestFor(source, "event-api");
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(plan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .doesNotContain("LB-DEPS-001", "LB-DEPS-002", "LB-FAPI-001");
        assertThat(plan.diagnostics()).anySatisfy(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo("LB-FAPI-100");
            assertThat(diagnostic.severity()).isEqualTo(DiagnosticSeverity.INFO);
            assertThat(diagnostic.message()).contains("fabric-api-base-bridge");
        });
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .contains("fabric-api-base-bridge-0.4.42_6573ed8c19-loaderbridge.1.jar");
        assertThat(Files.readString(request.outputDirectory().resolve("bridge.lock.json")))
                .contains("runtime-bridge-module", "fabric-api-base-bridge");
        try (JarFile jar = new JarFile(result.artifacts().stream()
                .filter(path -> path.getFileName().toString().startsWith("event_api-"))
                .findFirst().orElseThrow().toFile())) {
            String forgeMetadata = new String(jar.getInputStream(
                    jar.getJarEntry("META-INF/mods.toml")).readAllBytes(), StandardCharsets.UTF_8);
            assertThat(forgeMetadata).doesNotContain("fabric_api_base");
        }
    }

    @Test
    void automaticallySelectsLifecycleBridgeAndItsBaseDependency() throws Exception {
        Path source = referencedMod("lifecycle_api", "fabric-lifecycle-events-v1", writer -> {
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "references", "()V", null, null);
            method.visitFieldInsn(Opcodes.GETSTATIC,
                    "net/fabricmc/fabric/api/event/lifecycle/v1/ServerTickEvents",
                    "START_SERVER_TICK", "Lnet/fabricmc/fabric/api/event/Event;");
            method.visitInsn(Opcodes.POP);
            method.visitInsn(Opcodes.RETURN);
            method.visitMaxs(1, 1);
            method.visitEnd();
        });
        BridgeRequest request = requestFor(source, "lifecycle-api");
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(plan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .doesNotContain("LB-DEPS-001", "LB-FAPI-001", "LB-MODULE-003");
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .contains(
                        "fabric-api-base-bridge-0.4.42_6573ed8c19-loaderbridge.1.jar",
                        "fabric-lifecycle-events-bridge-2.6.0_0865547519-loaderbridge.6.jar");
        assertThat(Files.readString(request.outputDirectory().resolve("bridge.lock.json")))
                .contains("fabric-api-base-bridge", "fabric-lifecycle-events-bridge");
        try (JarFile jar = new JarFile(result.artifacts().stream()
                .filter(path -> path.getFileName().toString().startsWith("lifecycle_api-"))
                .findFirst().orElseThrow().toFile())) {
            String forgeMetadata = new String(jar.getInputStream(
                    jar.getJarEntry("META-INF/mods.toml")).readAllBytes(), StandardCharsets.UTF_8);
            assertThat(forgeMetadata).doesNotContain("fabric_lifecycle_events_v1");
        }
    }

    @Test
    void automaticallySelectsObjectBuilderBridgeFromBytecodeAndMetadata() throws Exception {
        Path source = referencedMod("object_builder_api", "fabric-object-builder-api-v1", writer -> {
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "references", "()V", null, null);
            method.visitInsn(Opcodes.ACONST_NULL);
            method.visitInsn(Opcodes.ICONST_0);
            method.visitTypeInsn(Opcodes.ANEWARRAY, "net/minecraft/world/level/block/Block");
            method.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "net/fabricmc/fabric/api/object/builder/v1/block/entity/FabricBlockEntityTypeBuilder",
                    "create",
                    "(Lnet/fabricmc/fabric/api/object/builder/v1/block/entity/"
                            + "FabricBlockEntityTypeBuilder$Factory;[Lnet/minecraft/world/level/block/Block;)"
                            + "Lnet/fabricmc/fabric/api/object/builder/v1/block/entity/"
                            + "FabricBlockEntityTypeBuilder;",
                    false);
            method.visitInsn(Opcodes.POP);
            method.visitInsn(Opcodes.RETURN);
            method.visitMaxs(2, 1);
            method.visitEnd();
        });
        BridgeRequest request = requestFor(source, "object-builder-api");
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(plan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .doesNotContain("LB-DEPS-001", "LB-FAPI-001", "LB-MODULE-003");
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .contains("fabric-object-builder-api-v1-bridge-15.2.1_40875a9319-loaderbridge.3.jar");
        assertThat(Files.readString(request.outputDirectory().resolve("bridge.lock.json")))
                .contains("fabric-object-builder-api-v1-bridge");
    }

    @Test
    void automaticallySelectsBlockLookupBridgeAndItsRuntimeDependencies() throws Exception {
        Path source = referencedMod("lookup_api", "fabric-api-lookup-api-v1", writer -> {
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "references", "()V", null, null);
            method.visitInsn(Opcodes.ACONST_NULL);
            method.visitTypeInsn(Opcodes.CHECKCAST,
                    "net/fabricmc/fabric/api/lookup/v1/block/BlockApiLookup");
            method.visitInsn(Opcodes.POP);
            method.visitInsn(Opcodes.RETURN);
            method.visitMaxs(1, 1);
            method.visitEnd();
        });
        BridgeRequest request = requestFor(source, "lookup-api");
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(plan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .doesNotContain("LB-DEPS-001", "LB-FAPI-001", "LB-MODULE-003");
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .contains(
                        "fabric-api-lookup-api-v1-bridge-1.6.72_d30f6a7919-loaderbridge.2.jar",
                        "fabric-api-base-bridge-0.4.42_6573ed8c19-loaderbridge.1.jar",
                        "fabric-lifecycle-events-bridge-2.6.0_0865547519-loaderbridge.6.jar");
        assertThat(Files.readString(request.outputDirectory().resolve("bridge.lock.json")))
                .contains("fabric-api-lookup-api-v1-bridge");
    }

    @Test
    void automaticallySelectsRegistrySyncBridgeAndItsBaseDependency() throws Exception {
        Path source = referencedMod("registry_sync", "fabric-registry-sync-v0", writer -> {
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "references", "()V", null, null);
            method.visitInsn(Opcodes.ACONST_NULL);
            method.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "net/fabricmc/fabric/api/event/registry/RegistryAttributeHolder", "get",
                    "(Lnet/minecraft/core/Registry;)"
                            + "Lnet/fabricmc/fabric/api/event/registry/RegistryAttributeHolder;",
                    true);
            method.visitInsn(Opcodes.POP);
            method.visitInsn(Opcodes.RETURN);
            method.visitMaxs(1, 1);
            method.visitEnd();
        });
        BridgeRequest request = requestFor(source, "registry-sync");
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(plan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .doesNotContain("LB-DEPS-001", "LB-FAPI-001", "LB-MODULE-003");
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .contains(
                "fabric-registry-sync-v0-bridge-5.1.3_60c3209b19-loaderbridge.5.jar",
                        "fabric-api-base-bridge-0.4.42_6573ed8c19-loaderbridge.1.jar");
        assertThat(Files.readString(request.outputDirectory().resolve("bridge.lock.json")))
                .contains("fabric-registry-sync-v0-bridge");
    }

    @Test
    void automaticallySelectsTransferTransactionBridgeFromBytecode() throws Exception {
        Path source = referencedMod("transfer_transaction", "fabric-transfer-api-v1", writer -> {
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "references", "()V", null, null);
            method.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "net/fabricmc/fabric/api/transfer/v1/transaction/Transaction", "openOuter",
                    "()Lnet/fabricmc/fabric/api/transfer/v1/transaction/Transaction;", true);
            method.visitInsn(Opcodes.POP);
            method.visitInsn(Opcodes.RETURN);
            method.visitMaxs(1, 1);
            method.visitEnd();
        });
        BridgeRequest request = requestFor(source, "transfer-transaction");
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(plan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .doesNotContain("LB-DEPS-001", "LB-FAPI-001", "LB-MODULE-003");
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .contains("fabric-transfer-api-v1-bridge-5.4.4_7b3d111d19-loaderbridge.1.jar");
        assertThat(Files.readString(request.outputDirectory().resolve("bridge.lock.json")))
                .contains("fabric-transfer-api-v1-bridge");
    }

    @Test
    void automaticallySelectsResourceLoaderBridgeAndItsBaseDependency() throws Exception {
        Path source = referencedMod("resource_loader_api", "fabric-resource-loader-v0", writer -> {
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "references", "()V", null, null);
            method.visitFieldInsn(Opcodes.GETSTATIC, "net/minecraft/server/packs/PackType",
                    "SERVER_DATA", "Lnet/minecraft/server/packs/PackType;");
            method.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "net/fabricmc/fabric/api/resource/ResourceManagerHelper", "get",
                    "(Lnet/minecraft/server/packs/PackType;)Lnet/fabricmc/fabric/api/resource/ResourceManagerHelper;",
                    true);
            method.visitInsn(Opcodes.POP);
            method.visitInsn(Opcodes.RETURN);
            method.visitMaxs(1, 1);
            method.visitEnd();
        });
        BridgeRequest request = requestFor(source, "resource-loader-api");
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(plan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .doesNotContain("LB-DEPS-001", "LB-FAPI-001", "LB-MODULE-003");
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .contains("fabric-api-base-bridge-0.4.42_6573ed8c19-loaderbridge.1.jar",
                        "fabric-resource-loader-v0-bridge-1.3.1_5b5275af19-loaderbridge.1.jar");
        assertThat(Files.readString(request.outputDirectory().resolve("bridge.lock.json")))
                .contains("fabric-api-base-bridge", "fabric-resource-loader-v0-bridge");
    }

    @Test
    void automaticallySelectsCommandBridgeAndItsBaseDependency() throws Exception {
        Path source = referencedMod("command_api", "fabric-command-api-v2", writer -> {
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "references", "()V", null, null);
            method.visitFieldInsn(Opcodes.GETSTATIC,
                    "net/fabricmc/fabric/api/command/v2/CommandRegistrationCallback",
                    "EVENT", "Lnet/fabricmc/fabric/api/event/Event;");
            method.visitInsn(Opcodes.POP);
            method.visitInsn(Opcodes.RETURN);
            method.visitMaxs(1, 1);
            method.visitEnd();
        });
        BridgeRequest request = requestFor(source, "command-api");
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(plan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .doesNotContain("LB-DEPS-001", "LB-FAPI-001", "LB-MODULE-003");
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .contains(
                        "fabric-api-base-bridge-0.4.42_6573ed8c19-loaderbridge.1.jar",
                        "fabric-command-api-v2-bridge-2.2.28_6ced4dd919-loaderbridge.1.jar");
        assertThat(Files.readString(request.outputDirectory().resolve("bridge.lock.json")))
                .contains("fabric-api-base-bridge", "fabric-command-api-v2-bridge");
    }

    @Test
    void rejectsOverlappingRuntimeBridgeModules() {
        RuntimeBridgeModuleProvider first = moduleProvider("first", "example.Shared", "example-api");
        RuntimeBridgeModuleProvider second = moduleProvider("second", "example.Shared", "other-api");

        assertThatThrownBy(() -> new FabricToForgeAdapter(
                (version, cache, refresh) -> { throw new AssertionError("unused"); },
                (version, cache) -> { throw new AssertionError("unused"); },
                (cache, refresh) -> { throw new AssertionError("unused"); },
                List.of(first, second)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LB-MODULE-001", "example.Shared");
    }

    @Test
    void recursivelyPreparesAndDeduplicatesNestedMods() throws Exception {
        byte[] child = jarBytes("""
                {"schemaVersion":1,"id":"nested_child","version":"1.0.0"}
                """);
        Path source = temporaryDirectory.resolve("nested-parent.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(source))) {
            jar.putNextEntry(new JarEntry("fabric.mod.json"));
            jar.write(("{\"schemaVersion\":1,\"id\":\"nested_parent\",\"version\":\"1.0.0\","
                    + "\"jars\":[{\"file\":\"META-INF/jars/child.jar\"},"
                    + "{\"file\":\"META-INF/jars/child-copy.jar\"}]}")
                    .getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
            for (String location : List.of("META-INF/jars/child.jar", "META-INF/jars/child-copy.jar")) {
                jar.putNextEntry(new JarEntry(location));
                jar.write(child);
                jar.closeEntry();
            }
        }
        BridgeRequest request = new BridgeRequest("1.21.1", new LoaderId("forge"), "52.1.0",
                BridgeEnvironment.CLIENT, List.of(source), temporaryDirectory.resolve("output-nested"),
                temporaryDirectory.resolve("cache-nested"));
        BridgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .containsExactlyInAnyOrder(
                        "nested_parent-1.0.0-loaderbridge.jar",
                        "nested_child-1.0.0-loaderbridge.jar");
        Path childOutput = result.artifacts().stream()
                .filter(path -> path.getFileName().toString().startsWith("nested_child-"))
                .findFirst().orElseThrow();
        try (JarFile jar = new JarFile(childOutput.toFile())) {
            String bridgeMetadata = new String(
                    jar.getInputStream(jar.getJarEntry("META-INF/loaderbridge.json")).readAllBytes(),
                    StandardCharsets.UTF_8);
            assertThat(bridgeMetadata).contains(
                    "\"parentModId\": \"nested_parent\"",
                    "\"parentSubLocation\": \"META-INF/jars/child.jar\"");
        }
    }

    @Test
    void deduplicatesSameVersionModulesByFabricIdAndRejectsVersionCollisions() throws Exception {
        byte[] first = jarBytes("""
                {"schemaVersion":1,"id":"fabric_api_module","version":"1.0.0"}
                """);
        byte[] sameIdentity = jarBytesWithResource("""
                {"schemaVersion":1,"id":"fabric_api_module","version":"1.0.0"}
                """, "different.txt", "different");
        byte[] conflicting = jarBytes("""
                {"schemaVersion":1,"id":"fabric_api_module","version":"2.0.0"}
                """);
        Path parent = nestedParent("dedup_parent", first, sameIdentity);
        BridgeRequest request = new BridgeRequest("1.21.1", new LoaderId("forge"), "52.1.0",
                BridgeEnvironment.CLIENT, List.of(parent), temporaryDirectory.resolve("output-id-dedup"),
                temporaryDirectory.resolve("cache-id-dedup"));

        var result = new FabricToForgeAdapter().prepare(request,
                new FabricToForgeAdapter().plan(request));
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .containsExactlyInAnyOrder("dedup_parent-1-loaderbridge.jar",
                        "fabric_api_module-1.0.0-loaderbridge.jar");

        Path collisionParent = nestedParent("collision_parent", first, conflicting);
        BridgeRequest collisionRequest = new BridgeRequest("1.21.1", new LoaderId("forge"), "52.1.0",
                BridgeEnvironment.CLIENT, List.of(collisionParent),
                temporaryDirectory.resolve("output-id-collision"),
                temporaryDirectory.resolve("cache-id-collision"));
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();
        var collisionPlan = adapter.plan(collisionRequest);
        assertThat(collisionPlan.canPrepare()).isFalse();
        assertThat(collisionPlan.diagnostics()).anySatisfy(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo("LB-NESTED-006");
            assertThat(diagnostic.message()).contains("conflicting versions");
        });
    }

    private Path nestedParent(String id, byte[] first, byte[] second) throws Exception {
        Path source = temporaryDirectory.resolve(id + ".jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(source))) {
            jar.putNextEntry(new JarEntry("fabric.mod.json"));
            jar.write(("{\"schemaVersion\":1,\"id\":\"" + id + "\",\"version\":\"1\","
                    + "\"jars\":[{\"file\":\"META-INF/jars/first.jar\"},"
                    + "{\"file\":\"META-INF/jars/second.jar\"}]}")
                    .getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("META-INF/jars/first.jar"));
            jar.write(first);
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("META-INF/jars/second.jar"));
            jar.write(second);
            jar.closeEntry();
        }
        return source;
    }

    private Path referencedMod(String id, String fabricDependency,
            java.util.function.Consumer<ClassWriter> classBody) throws Exception {
        Path source = temporaryDirectory.resolve(id + ".jar");
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "fixture/References", null,
                "java/lang/Object", null);
        classBody.accept(writer);
        writer.visitEnd();
        String depends = fabricDependency == null ? "" : ",\"depends\":{\""
                + fabricDependency + "\":\"*\"}";
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(source))) {
            jar.putNextEntry(new JarEntry("fabric.mod.json"));
            jar.write(("{\"schemaVersion\":1,\"id\":\"" + id
                    + "\",\"version\":\"1\"" + depends + "}")
                    .getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("fixture/References.class"));
            jar.write(writer.toByteArray());
            jar.closeEntry();
        }
        return source;
    }

    private BridgeRequest requestFor(Path source, String directory) {
        return new BridgeRequest("1.21.1", new LoaderId("forge"), "52.1.0",
                BridgeEnvironment.SERVER, List.of(source), temporaryDirectory.resolve(directory),
                temporaryDirectory.resolve(directory + "-cache"));
    }

    private RuntimeBridgeModuleProvider moduleProvider(String id, String className, String modId) {
        return new RuntimeBridgeModuleProvider() {
            @Override
            public RuntimeBridgeModule descriptor() {
                return new RuntimeBridgeModule(id, "test:1", "1.0.0",
                        BridgeCapability.FABRIC_API, Set.of(className), Map.of(modId, "1.0.0"),
                        Set.of());
            }

            @Override
            public Path artifact() {
                return temporaryDirectory.resolve(id + ".jar");
            }
        };
    }

    private static byte[] jarBytes(String metadata) throws Exception {
        return jarBytesWithResource(metadata, null, null);
    }

    private static byte[] jarBytesWithResource(String metadata, String resource, String contents)
            throws Exception {
        var output = new java.io.ByteArrayOutputStream();
        try (JarOutputStream jar = new JarOutputStream(output)) {
            jar.putNextEntry(new JarEntry("fabric.mod.json"));
            jar.write(metadata.getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
            if (resource != null) {
                jar.putNextEntry(new JarEntry(resource));
                jar.write(contents.getBytes(StandardCharsets.UTF_8));
                jar.closeEntry();
            }
        }
        return output.toByteArray();
    }
}
