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
                .contains("sourceSha256", "outputSha256", "\"adapterVersion\": \"0.5.0\"",
                        "adapterArtifactSha256");
        try (JarFile jar = new JarFile(result.artifacts().getFirst().toFile())) {
            assertThat(jar.getEntry("pack.mcmeta")).isNotNull();
            assertThat(new String(jar.getInputStream(jar.getJarEntry("META-INF/loaderbridge.json"))
                    .readAllBytes(), StandardCharsets.UTF_8))
                    .contains("\"sourceNamespace\": \"neutral\"");
        }
    }

    @Test
    void plansAgainstTheSameFabricLoaderVersionExposedAtRuntime() throws Exception {
        Path compatible = fabricLoaderDependentMod("compatible_loader", ">=0.16.14");
        Path future = fabricLoaderDependentMod("future_loader", ">=0.16.15");

        var compatiblePlan = new FabricToForgeAdapter().plan(
                requestFor(compatible, "compatible-loader"));
        var futurePlan = new FabricToForgeAdapter().plan(requestFor(future, "future-loader"));

        assertThat(compatiblePlan.canPrepare()).isTrue();
        assertThat(compatiblePlan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .doesNotContain("LB-DEPS-002");
        assertThat(futurePlan.canPrepare()).isFalse();
        assertThat(futurePlan.diagnostics()).anySatisfy(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo("LB-DEPS-002");
            assertThat(diagnostic.message()).contains("fabricloader has 0.16.14");
        });
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
    void rejectsDuplicateActiveMixinConfigurationNamesAcrossMods() throws Exception {
        Path first = temporaryDirectory.resolve("first-mixin-config.jar");
        Files.write(first, jarBytes("""
                {"schemaVersion":1,"id":"first_mixin_owner","version":"1",
                 "mixins":["shared.mixins.json"]}
                """));
        Path second = temporaryDirectory.resolve("second-mixin-config.jar");
        Files.write(second, jarBytes("""
                {"schemaVersion":1,"id":"second_mixin_owner","version":"1",
                 "mixins":[{"config":"shared.mixins.json","environment":"server"}]}
                """));
        BridgeRequest request = new BridgeRequest("1.21.1", new LoaderId("forge"), "52.1.0",
                BridgeEnvironment.SERVER, List.of(first, second),
                temporaryDirectory.resolve("duplicate-mixin-output"),
                temporaryDirectory.resolve("duplicate-mixin-cache"));

        var plan = new FabricToForgeAdapter().plan(request);
        BridgeRequest clientRequest = new BridgeRequest("1.21.1", new LoaderId("forge"),
                "52.1.0", BridgeEnvironment.CLIENT, List.of(first, second),
                temporaryDirectory.resolve("sided-mixin-output"),
                temporaryDirectory.resolve("sided-mixin-cache"));
        var clientPlan = new FabricToForgeAdapter().plan(clientRequest);

        assertThat(plan.canPrepare()).isFalse();
        assertThat(plan.diagnostics()).anySatisfy(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo("LB-MIXIN-002");
            assertThat(diagnostic.modId()).isEqualTo("second_mixin_owner");
            assertThat(diagnostic.message()).contains(
                    "shared.mixins.json", "first_mixin_owner");
        });
        assertThat(clientPlan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .doesNotContain("LB-MIXIN-002");
    }

    @Test
    void diagnosesOnlyLegacyFabricLocalVariableMixinSemantics() throws Exception {
        Path legacy = localVariableMixinMod("legacy_locals", "*");
        Path modern = localVariableMixinMod("modern_locals", ">=0.16.0");

        var legacyPlan = new FabricToForgeAdapter().plan(requestFor(legacy, "legacy-locals"));
        var modernPlan = new FabricToForgeAdapter().plan(requestFor(modern, "modern-locals"));

        assertThat(legacyPlan.canPrepare()).isFalse();
        assertThat(legacyPlan.diagnostics()).anySatisfy(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo("LB-MIXIN-017");
            assertThat(diagnostic.message()).contains("pre-0.12", "modify-variable");
        });
        assertThat(modernPlan.canPrepare()).isTrue();
        assertThat(modernPlan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .doesNotContain("LB-MIXIN-017");
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
                    "net/fabricmc/fabric/api/transfer/v1/client/fluid/FluidVariantRendering",
                    "find", "()V", false);
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
    void analyzesFabricApiRequirementsInSelectedNestedMods() throws Exception {
        Path nested = referencedMod("nested_optional_api", null, writer -> {
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "references", "()V", null, null);
            method.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "net/fabricmc/fabric/api/transfer/v1/client/fluid/FluidVariantRendering",
                    "find", "()V", false);
            method.visitEnd();
        });
        Path parent = temporaryDirectory.resolve("nested-api-parent.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(parent))) {
            jar.putNextEntry(new JarEntry("fabric.mod.json"));
            jar.write("""
                    {"schemaVersion":1,"id":"nested_api_parent","version":"1",
                     "jars":[{"file":"META-INF/jars/child.jar"}]}
                    """.getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("META-INF/jars/child.jar"));
            jar.write(Files.readAllBytes(nested));
            jar.closeEntry();
        }

        var plan = new FabricToForgeAdapter().plan(requestFor(parent, "nested-api"));

        assertThat(plan.diagnostics()).anySatisfy(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo("LB-FAPI-002");
            assertThat(diagnostic.modId()).isEqualTo("nested_optional_api");
        });
    }

    @Test
    void analyzesLanguageAdaptersInSelectedNestedMods() throws Exception {
        byte[] nested = jarBytes("""
                {"schemaVersion":1,"id":"nested_custom_adapter","version":"1",
                 "languageAdapters":{"custom":"fixture.CustomAdapter"},
                 "entrypoints":{"main":[{"adapter":"custom","value":"fixture.Main"}]}}
                """);
        Path parent = temporaryDirectory.resolve("nested-adapter-parent.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(parent))) {
            jar.putNextEntry(new JarEntry("fabric.mod.json"));
            jar.write("""
                    {"schemaVersion":1,"id":"nested_adapter_parent","version":"1",
                     "jars":[{"file":"META-INF/jars/child.jar"}]}
                    """.getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("META-INF/jars/child.jar"));
            jar.write(nested);
            jar.closeEntry();
        }

        var plan = new FabricToForgeAdapter().plan(requestFor(parent, "nested-adapter"));

        assertThat(plan.canPrepare()).isFalse();
        assertThat(plan.diagnostics()).anySatisfy(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo("LB-LANG-001");
            assertThat(diagnostic.modId()).isEqualTo("nested_custom_adapter");
            assertThat(diagnostic.message()).contains("custom");
        });
    }

    @Test
    void rejectsReservedAndDuplicateLanguageAdapterDefinitions() throws Exception {
        Path reserved = temporaryDirectory.resolve("reserved-adapter.jar");
        Files.write(reserved, jarBytes("""
                {"schemaVersion":1,"id":"reserved_adapter","version":"1",
                 "languageAdapters":{"default":"fixture.Replacement"}}
                """));
        Path first = temporaryDirectory.resolve("first-kotlin-adapter.jar");
        Files.write(first, jarBytes("""
                {"schemaVersion":1,"id":"first_kotlin_adapter","version":"1",
                 "languageAdapters":{"kotlin":"fixture.First"}}
                """));
        Path second = temporaryDirectory.resolve("second-kotlin-adapter.jar");
        Files.write(second, jarBytes("""
                {"schemaVersion":1,"id":"second_kotlin_adapter","version":"1",
                 "languageAdapters":{"kotlin":"fixture.Second"}}
                """));
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var reservedPlan = adapter.plan(requestFor(reserved, "reserved-adapter"));
        BridgeRequest duplicateRequest = new BridgeRequest("1.21.1", new LoaderId("forge"),
                "52.1.0", BridgeEnvironment.SERVER, List.of(first, second),
                temporaryDirectory.resolve("duplicate-adapter"),
                temporaryDirectory.resolve("duplicate-adapter-cache"));
        var duplicatePlan = adapter.plan(duplicateRequest);

        assertThat(reservedPlan.canPrepare()).isFalse();
        assertThat(reservedPlan.diagnostics()).anySatisfy(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo("LB-LANG-002");
            assertThat(diagnostic.modId()).isEqualTo("reserved_adapter");
            assertThat(diagnostic.message()).contains("default", "fabricloader");
        });
        assertThat(duplicatePlan.canPrepare()).isFalse();
        assertThat(duplicatePlan.diagnostics()).anySatisfy(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo("LB-LANG-002");
            assertThat(diagnostic.modId()).isEqualTo("second_kotlin_adapter");
            assertThat(diagnostic.message()).contains("kotlin", "first_kotlin_adapter");
        });
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
    void automaticallySelectsResourceConditionsBridgeFromConditionalJson() throws Exception {
        Path source = temporaryDirectory.resolve("conditional_resources.jar");
        Files.write(source, jarBytesWithResource(
                "{\"schemaVersion\":1,\"id\":\"conditional_resources\",\"version\":\"1\"}",
                "data/fixture/recipe/optional.json",
                """
                        {"type":"missing:serializer","fabric:load_conditions":[
                          {"condition":"fabric:all_mods_loaded","values":["missing"]}
                        ]}
                        """));
        BridgeRequest request = requestFor(source, "conditional-resources");
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(plan.diagnostics()).anySatisfy(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo("LB-FAPI-100");
            assertThat(diagnostic.message()).contains("fabric-resource-conditions-api-v1-bridge");
        });
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .contains("fabric-resource-conditions-api-v1-bridge-4.3.0_8dc279b119-loaderbridge.1.jar");
    }

    @Test
    void automaticallySelectsLootApiBridgeAndItsRuntimeDependencies() throws Exception {
        Path source = referencedMod("loot_api", "fabric-loot-api-v3", writer -> {
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "references", "()V", null, null);
            method.visitFieldInsn(Opcodes.GETSTATIC,
                    "net/fabricmc/fabric/api/loot/v3/LootTableEvents",
                    "MODIFY", "Lnet/fabricmc/fabric/api/event/Event;");
            method.visitInsn(Opcodes.POP);
            method.visitInsn(Opcodes.RETURN);
            method.visitMaxs(1, 1);
            method.visitEnd();
        });
        BridgeRequest request = requestFor(source, "loot-api");
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(plan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .doesNotContain("LB-DEPS-001", "LB-FAPI-001", "LB-MODULE-003");
        assertThat(plan.diagnostics()).anySatisfy(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo("LB-FAPI-100");
            assertThat(diagnostic.message()).contains("fabric-loot-api-v3-bridge");
        });
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .contains(
                        "fabric-loot-api-v3-bridge-1.0.3_3f89f5a519-loaderbridge.1.jar",
                        "fabric-api-base-bridge-0.4.42_6573ed8c19-loaderbridge.1.jar",
                        "fabric-resource-loader-v0-bridge-1.3.1_5b5275af19-loaderbridge.1.jar");
    }

    @Test
    void automaticallySelectsRecipeApiBridgeAndItsTransitiveRuntimeDependencies() throws Exception {
        Path source = referencedMod("recipe_api", "fabric-recipe-api-v1", writer -> {
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "references", "()V", null, null);
            method.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "net/fabricmc/fabric/api/recipe/v1/ingredient/DefaultCustomIngredients",
                    "any",
                    "([Lnet/minecraft/class_1856;)Lnet/minecraft/class_1856;",
                    false);
            method.visitInsn(Opcodes.POP);
            method.visitInsn(Opcodes.RETURN);
            method.visitMaxs(1, 1);
            method.visitEnd();
        });
        BridgeRequest request = requestFor(source, "recipe-api");
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(plan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .doesNotContain("LB-DEPS-001", "LB-FAPI-001", "LB-MODULE-003");
        assertThat(plan.diagnostics()).anySatisfy(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo("LB-FAPI-100");
            assertThat(diagnostic.message()).contains("fabric-recipe-api-v1-bridge");
        });
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .contains(
                        "fabric-recipe-api-v1-bridge-5.0.16_2475392c19-loaderbridge.1.jar",
                        "fabric-networking-api-v1-bridge-4.3.1_d30f6a7919-loaderbridge.5.jar",
                        "fabric-api-base-bridge-0.4.42_6573ed8c19-loaderbridge.1.jar");
    }

    @Test
    void automaticallySelectsBlockApiBridgeFromInterfaceReference() throws Exception {
        Path source = referencedMod("block_api", "fabric-block-api-v1", writer -> {
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "appearance",
                    "(Lnet/fabricmc/fabric/api/block/v1/FabricBlock;)V", null, null);
            method.visitInsn(Opcodes.RETURN);
            method.visitMaxs(0, 2);
            method.visitEnd();
        });
        BridgeRequest request = requestFor(source, "block-api");
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(plan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .doesNotContain("LB-DEPS-001", "LB-FAPI-001", "LB-FAPI-002", "LB-MODULE-003");
        assertThat(plan.diagnostics()).anySatisfy(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo("LB-FAPI-100");
            assertThat(diagnostic.message()).contains("fabric-block-api-v1-bridge");
        });
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .contains("fabric-block-api-v1-bridge-1.1.0_0bc3503219-loaderbridge.1.jar");
    }

    @Test
    void automaticallySelectsServerInteractionEventsBridge() throws Exception {
        Path source = referencedMod("interaction_events", "fabric-events-interaction-v0", writer -> {
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "callback",
                    "(Lnet/fabricmc/fabric/api/event/player/UseBlockCallback;)V", null, null);
            method.visitInsn(Opcodes.RETURN);
            method.visitMaxs(0, 2);
            method.visitEnd();
        });
        BridgeRequest request = requestFor(source, "interaction-events");
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(plan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .doesNotContain("LB-DEPS-001", "LB-FAPI-001", "LB-FAPI-002", "LB-MODULE-003");
        assertThat(plan.diagnostics()).anySatisfy(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo("LB-FAPI-100");
            assertThat(diagnostic.message()).contains("fabric-events-interaction-v0-bridge");
        });
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .contains(
                        "fabric-events-interaction-v0-bridge-0.7.14_ba9dae0619-loaderbridge.2.jar",
                        "fabric-api-base-bridge-0.4.42_6573ed8c19-loaderbridge.1.jar");
    }

    @Test
    void automaticallySelectsItemApiBridgeFromInjectedInterfaceReference() throws Exception {
        Path source = referencedMod("item_api", "fabric-item-api-v1", writer -> {
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "item",
                    "(Lnet/fabricmc/fabric/api/item/v1/FabricItemStack;)V", null, null);
            method.visitInsn(Opcodes.RETURN);
            method.visitMaxs(0, 2);
            method.visitEnd();
        });
        BridgeRequest request = requestFor(source, "item-api");
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(plan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .doesNotContain("LB-DEPS-001", "LB-FAPI-001", "LB-FAPI-002", "LB-MODULE-003");
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .contains("fabric-item-api-v1-bridge-11.3.0_467044f319-loaderbridge.5.jar");
    }

    @Test
    void automaticallySelectsInteractionBridgeForFakePlayerReference() throws Exception {
        Path source = referencedMod("fake_player", "fabric-events-interaction-v0", writer -> {
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "player",
                    "()Lnet/fabricmc/fabric/api/entity/FakePlayer;", null, null);
            method.visitInsn(Opcodes.ACONST_NULL);
            method.visitInsn(Opcodes.ARETURN);
            method.visitMaxs(1, 1);
            method.visitEnd();
        });
        BridgeRequest request = requestFor(source, "fake-player");
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(plan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .doesNotContain("LB-DEPS-001", "LB-FAPI-001", "LB-FAPI-002", "LB-MODULE-003");
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .contains("fabric-events-interaction-v0-bridge-0.7.14_ba9dae0619-loaderbridge.2.jar");
    }

    @Test
    void automaticallySelectsEntityEventsBridgeForNestedCallbackReferences() throws Exception {
        Path source = referencedMod("entity_events_api", "fabric-entity-events-v1", writer -> {
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "callback",
                    "(Lnet/fabricmc/fabric/api/entity/event/v1/"
                            + "ServerLivingEntityEvents$AllowDamage;)V",
                    null, null);
            method.visitInsn(Opcodes.RETURN);
            method.visitMaxs(0, 2);
            method.visitEnd();
        });
        BridgeRequest request = requestFor(source, "entity-events-api");
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(plan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .doesNotContain("LB-DEPS-001", "LB-FAPI-001", "LB-FAPI-002", "LB-MODULE-003");
        assertThat(plan.diagnostics()).anySatisfy(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo("LB-FAPI-100");
            assertThat(diagnostic.message()).contains("fabric-entity-events-v1-bridge");
        });
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .contains(
                        "fabric-entity-events-v1-bridge-1.8.0_2b27e0a419-loaderbridge.2.jar",
                        "fabric-api-base-bridge-0.4.42_6573ed8c19-loaderbridge.1.jar");
    }

    @Test
    void automaticallySelectsDataAttachmentBridgeAndItsRuntimeDependencies() throws Exception {
        Path source = referencedMod("data_attachment_api", "fabric-data-attachment-api-v1",
                writer -> {
                    var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "attachment",
                            "(Lnet/fabricmc/fabric/api/attachment/v1/AttachmentType;)V",
                            null, null);
                    method.visitInsn(Opcodes.RETURN);
                    method.visitMaxs(0, 2);
                    method.visitEnd();
                });
        BridgeRequest request = requestFor(source, "data-attachment-api");
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(plan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .doesNotContain("LB-DEPS-001", "LB-FAPI-001", "LB-FAPI-002", "LB-MODULE-003");
        assertThat(plan.diagnostics()).anySatisfy(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo("LB-FAPI-100");
            assertThat(diagnostic.message()).contains("fabric-data-attachment-api-v1-bridge");
        });
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .contains(
                        "fabric-data-attachment-api-v1-bridge-1.4.7_5b36e0f719-loaderbridge.9.jar",
                        "fabric-entity-events-v1-bridge-1.8.0_2b27e0a419-loaderbridge.2.jar",
                        "fabric-object-builder-api-v1-bridge-15.2.1_40875a9319-loaderbridge.6.jar",
                        "fabric-networking-api-v1-bridge-4.3.1_d30f6a7919-loaderbridge.5.jar");
    }

    @Test
    void rejectsConditionalPackOverlaysUntilTheirPackSelectionHookExists() throws Exception {
        Path source = temporaryDirectory.resolve("conditional_overlay.jar");
        Files.write(source, jarBytesWithResource(
                "{\"schemaVersion\":1,\"id\":\"conditional_overlay\",\"version\":\"1\"}",
                "pack.mcmeta",
                "{\"pack\":{\"pack_format\":48,\"description\":\"fixture\"},"
                        + "\"fabric:overlays\":[]}"));
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(requestFor(source, "conditional-overlay"));

        assertThat(plan.canPrepare()).isFalse();
        assertThat(plan.diagnostics()).anySatisfy(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo("LB-FAPI-004");
            assertThat(diagnostic.severity()).isEqualTo(DiagnosticSeverity.ERROR);
            assertThat(diagnostic.message()).contains("conditional resource-pack overlays");
        });
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
                .contains("fabric-object-builder-api-v1-bridge-15.2.1_40875a9319-loaderbridge.6.jar");
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
                .contains("fabric-transfer-api-v1-bridge-5.4.4_7b3d111d19-loaderbridge.10.jar");
        assertThat(Files.readString(request.outputDirectory().resolve("bridge.lock.json")))
                .contains("fabric-transfer-api-v1-bridge");
    }

    @Test
    void automaticallySelectsContentRegistriesBridgeFromBytecode() throws Exception {
        Path source = referencedMod("content_registries", "fabric-content-registries-v0",
                writer -> {
                    var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "references", "()V",
                            null, null);
                    method.visitFieldInsn(Opcodes.GETSTATIC,
                            "net/fabricmc/fabric/api/registry/FuelRegistry", "INSTANCE",
                            "Lnet/fabricmc/fabric/api/registry/FuelRegistry;");
                    method.visitInsn(Opcodes.POP);
                    method.visitInsn(Opcodes.RETURN);
                    method.visitMaxs(1, 1);
                    method.visitEnd();
                });
        BridgeRequest request = requestFor(source, "content-registries");
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(plan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .doesNotContain("LB-DEPS-001", "LB-FAPI-001", "LB-MODULE-003");
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .contains("fabric-content-registries-v0-bridge-8.0.19_b559734419-loaderbridge.2.jar");
        assertThat(Files.readString(request.outputDirectory().resolve("bridge.lock.json")))
                .contains("fabric-content-registries-v0-bridge");
    }

    @Test
    void automaticallySelectsItemGroupBridgeFromBytecode() throws Exception {
        Path source = referencedMod("item_group", "fabric-item-group-api-v1", writer -> {
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "references", "()V", null, null);
            method.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "net/fabricmc/fabric/api/itemgroup/v1/FabricItemGroup", "builder",
                    "()Lnet/minecraft/world/item/CreativeModeTab$Builder;", false);
            method.visitInsn(Opcodes.POP);
            method.visitInsn(Opcodes.RETURN);
            method.visitMaxs(1, 1);
            method.visitEnd();
        });
        BridgeRequest request = requestFor(source, "item-group");
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(plan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .doesNotContain("LB-DEPS-001", "LB-FAPI-001", "LB-MODULE-003");
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .contains("fabric-item-group-api-v1-bridge-4.1.7_def88e3a19-loaderbridge.1.jar");
    }

    @Test
    void automaticallySelectsConventionTagsBridgeFromBytecode() throws Exception {
        Path source = referencedMod("convention_tags", "fabric-convention-tags-v2", writer -> {
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "references", "()V", null, null);
            method.visitFieldInsn(Opcodes.GETSTATIC,
                    "net/fabricmc/fabric/api/tag/convention/v2/ConventionalBiomeTags", "IS_PLAINS",
                    "Lnet/minecraft/tags/TagKey;");
            method.visitInsn(Opcodes.POP);
            method.visitInsn(Opcodes.RETURN);
            method.visitMaxs(1, 1);
            method.visitEnd();
        });
        BridgeRequest request = requestFor(source, "convention-tags");
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(plan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .doesNotContain("LB-DEPS-001", "LB-FAPI-001", "LB-MODULE-003");
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .contains("fabric-convention-tags-v2-bridge-2.12.0_c3656daa19-loaderbridge.1.jar");
    }

    @Test
    void automaticallySelectsBiomeBridgeFromBytecode() throws Exception {
        Path source = referencedMod("biome_api", "fabric-biome-api-v1", writer -> {
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "references", "()V", null, null);
            method.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "net/fabricmc/fabric/api/biome/v1/BiomeSelectors", "foundInOverworld",
                    "()Ljava/util/function/Predicate;", false);
            method.visitInsn(Opcodes.POP);
            method.visitInsn(Opcodes.RETURN);
            method.visitMaxs(1, 1);
            method.visitEnd();
        });
        BridgeRequest request = requestFor(source, "biome-api");
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(plan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .doesNotContain("LB-DEPS-001", "LB-FAPI-001", "LB-MODULE-003");
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .contains("fabric-biome-api-v1-bridge-13.0.31_d527f9fd19-loaderbridge.1.jar");
    }

    @Test
    void automaticallySelectsBlockRenderLayerBridgeFromBytecode() throws Exception {
        Path source = referencedMod("block_render_layer", "fabric-blockrenderlayer-v1", writer -> {
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "references", "()V", null, null);
            method.visitFieldInsn(Opcodes.GETSTATIC,
                    "net/fabricmc/fabric/api/blockrenderlayer/v1/BlockRenderLayerMap", "INSTANCE",
                    "Lnet/fabricmc/fabric/api/blockrenderlayer/v1/BlockRenderLayerMap;");
            method.visitInsn(Opcodes.POP);
            method.visitInsn(Opcodes.RETURN);
            method.visitMaxs(1, 1);
            method.visitEnd();
        });
        BridgeRequest request = requestFor(source, "block-render-layer");
        var result = new FabricToForgeAdapter().prepare(request, new FabricToForgeAdapter().plan(request));
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .contains("fabric-blockrenderlayer-v1-bridge-1.1.52_0af3f5a719-loaderbridge.1.jar");
    }

    @Test
    void automaticallySelectsRenderingBridgeFromBytecode() throws Exception {
        Path source = referencedMod("rendering", "fabric-rendering-v1", writer -> {
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "references", "()V", null, null);
            method.visitFieldInsn(Opcodes.GETSTATIC,
                    "net/fabricmc/fabric/api/client/rendering/v1/ColorProviderRegistry", "BLOCK",
                    "Lnet/fabricmc/fabric/api/client/rendering/v1/ColorProviderRegistry;");
            method.visitInsn(Opcodes.POP);
            method.visitInsn(Opcodes.RETURN);
            method.visitMaxs(1, 1);
            method.visitEnd();
        });
        BridgeRequest request = requestFor(source, "rendering");
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();
        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);
        assertThat(plan.canPrepare()).isTrue();
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .contains("fabric-rendering-v1-bridge-5.1.0_ab4c25a019-loaderbridge.3.jar");
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .anyMatch(name -> name.startsWith("fabric-api-base-bridge-"));
    }

    @Test
    void itemStorageSelectionPullsLookupDependenciesAutomatically() throws Exception {
        Path source = referencedMod("item_storage", "fabric-transfer-api-v1", writer -> {
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "references", "()V", null, null);
            method.visitFieldInsn(Opcodes.GETSTATIC,
                    "net/fabricmc/fabric/api/transfer/v1/item/ItemStorage", "SIDED",
                    "Lnet/fabricmc/fabric/api/lookup/v1/block/BlockApiLookup;");
            method.visitInsn(Opcodes.POP);
            method.visitInsn(Opcodes.RETURN);
            method.visitMaxs(1, 1);
            method.visitEnd();
        });
        BridgeRequest request = requestFor(source, "item-storage");
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .contains(
                        "fabric-transfer-api-v1-bridge-5.4.4_7b3d111d19-loaderbridge.10.jar",
                        "fabric-api-lookup-api-v1-bridge-1.6.72_d30f6a7919-loaderbridge.2.jar",
                        "fabric-api-base-bridge-0.4.42_6573ed8c19-loaderbridge.1.jar",
                        "fabric-lifecycle-events-bridge-2.6.0_0865547519-loaderbridge.6.jar");
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
    void promotesAnIdenticalNestedArtifactWhenItIsAlsoInstalledAsARoot() throws Exception {
        byte[] child = jarBytes("""
                {"schemaVersion":1,"id":"promoted_child","version":"1.0.0"}
                """);
        Path childRoot = temporaryDirectory.resolve("promoted-child-root.jar");
        Files.write(childRoot, child);
        Path parent = temporaryDirectory.resolve("promotion-parent.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(parent))) {
            jar.putNextEntry(new JarEntry("fabric.mod.json"));
            jar.write("""
                    {"schemaVersion":1,"id":"promotion_parent","version":"1.0.0",
                     "jars":[{"file":"META-INF/jars/child.jar"}]}
                    """.getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("META-INF/jars/child.jar"));
            jar.write(child);
            jar.closeEntry();
        }
        BridgeRequest request = new BridgeRequest("1.21.1", new LoaderId("forge"), "52.1.0",
                BridgeEnvironment.CLIENT, List.of(parent, childRoot),
                temporaryDirectory.resolve("output-root-promotion"),
                temporaryDirectory.resolve("cache-root-promotion"));
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var result = adapter.prepare(request, adapter.plan(request));

        Path preparedChild = result.artifacts().stream()
                .filter(path -> path.getFileName().toString().startsWith("promoted_child-"))
                .findFirst().orElseThrow();
        try (JarFile jar = new JarFile(preparedChild.toFile())) {
            String bridgeMetadata = new String(jar.getInputStream(
                    jar.getJarEntry("META-INF/loaderbridge.json")).readAllBytes(),
                    StandardCharsets.UTF_8);
            assertThat(bridgeMetadata).doesNotContain("parentModId", "parentSubLocation");
        }
    }

    @Test
    void filtersIncompatibleRootModsBeforeDependencyResolutionAndPreparation() throws Exception {
        Path clientOnly = temporaryDirectory.resolve("client-only.jar");
        Files.write(clientOnly, jarBytes("""
                {"schemaVersion":1,"id":"client_only","version":"1.0.0",
                 "environment":"client","depends":{"missing_client_library":"*"}}
                """));
        BridgeRequest serverRequest = new BridgeRequest(
                "1.21.1", new LoaderId("forge"), "52.1.0", BridgeEnvironment.SERVER,
                List.of(clientOnly), temporaryDirectory.resolve("server-filter-output"),
                temporaryDirectory.resolve("server-filter-cache"));
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(serverRequest);
        var result = adapter.prepare(serverRequest, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(plan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .contains("LB-ENV-100")
                .doesNotContain("LB-DEPS-001");
        assertThat(result.artifacts()).isEmpty();
    }

    @Test
    void filtersIncompatibleNestedModsForTheRequestedSide() throws Exception {
        byte[] clientChild = jarBytes("""
                {"schemaVersion":1,"id":"client_nested_child","version":"1.0.0",
                 "environment":"client"}
                """ );
        Path parent = temporaryDirectory.resolve("sided-nested-parent.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(parent))) {
            jar.putNextEntry(new JarEntry("fabric.mod.json"));
            jar.write("""
                    {"schemaVersion":1,"id":"sided_parent","version":"1.0.0",
                     "jars":[{"file":"META-INF/jars/client-child.jar"}]}
                    """.getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("META-INF/jars/client-child.jar"));
            jar.write(clientChild);
            jar.closeEntry();
        }
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();
        Path sharedOutput = temporaryDirectory.resolve("nested-sided-output");
        Path sharedCache = temporaryDirectory.resolve("nested-sided-cache");
        BridgeRequest serverRequest = new BridgeRequest(
                "1.21.1", new LoaderId("forge"), "52.1.0", BridgeEnvironment.SERVER,
                List.of(parent), sharedOutput, sharedCache);
        BridgeRequest clientRequest = new BridgeRequest(
                "1.21.1", new LoaderId("forge"), "52.1.0", BridgeEnvironment.CLIENT,
                List.of(parent), sharedOutput, sharedCache);

        var clientResult = adapter.prepare(clientRequest, adapter.plan(clientRequest));
        var serverResult = adapter.prepare(serverRequest, adapter.plan(serverRequest));

        assertThat(serverResult.artifacts()).extracting(path -> path.getFileName().toString())
                .containsExactly("sided_parent-1.0.0-loaderbridge.jar");
        assertThat(clientResult.artifacts()).extracting(path -> path.getFileName().toString())
                .containsExactlyInAnyOrder(
                        "sided_parent-1.0.0-loaderbridge.jar",
                        "client_nested_child-1.0.0-loaderbridge.jar");
        assertThat(sharedOutput.resolve("client_nested_child-1.0.0-loaderbridge.jar"))
                .doesNotExist();
        assertThat(Files.readString(sharedOutput.resolve("bridge.lock.json")))
                .contains("\"environment\": \"SERVER\"");
    }

    @Test
    void neverDeletesLockEntriesOutsideTheManagedOutputDirectory() throws Exception {
        Path source = temporaryDirectory.resolve("safe-mod.jar");
        Files.write(source, jarBytes("""
                {"schemaVersion":1,"id":"safe_mod","version":"1.0.0"}
                """));
        Path output = temporaryDirectory.resolve("managed-output");
        Files.createDirectories(output);
        Path outside = temporaryDirectory.resolve("outside-user-file.jar");
        Files.writeString(outside, "user-owned", StandardCharsets.UTF_8);
        Files.writeString(output.resolve("bridge.lock.json"), """
                {"adapter":"fabric-to-forge","artifacts":[{"output":"%s"}]}
                """.formatted(outside), StandardCharsets.UTF_8);
        BridgeRequest request = new BridgeRequest(
                "1.21.1", new LoaderId("forge"), "52.1.0", BridgeEnvironment.CLIENT,
                List.of(source), output, temporaryDirectory.resolve("safe-cache"));
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        adapter.prepare(request, adapter.plan(request));

        assertThat(outside).exists().content(StandardCharsets.UTF_8).isEqualTo("user-owned");
    }

    @Test
    void ordersForgeContainersAfterTheCanonicalProviderOfAFabricAlias() throws Exception {
        Path provider = temporaryDirectory.resolve("alias-provider.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(provider))) {
            jar.putNextEntry(new JarEntry("fabric.mod.json"));
            jar.write("""
                    {"schemaVersion":1,"id":"canonical_provider","version":"1.0.0",
                     "provides":["provided_api"]}
                    """.getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
        Path consumer = temporaryDirectory.resolve("alias-consumer.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(consumer))) {
            jar.putNextEntry(new JarEntry("fabric.mod.json"));
            jar.write("""
                    {"schemaVersion":1,"id":"alias_consumer","version":"1.0.0",
                     "depends":{"provided_api":">=1.0.0"}}
                    """.getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
        BridgeRequest request = new BridgeRequest("1.21.1", new LoaderId("forge"), "52.1.0",
                BridgeEnvironment.SERVER, List.of(provider, consumer),
                temporaryDirectory.resolve("alias-output"),
                temporaryDirectory.resolve("alias-cache"));
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        Path consumerOutput = result.artifacts().stream()
                .filter(path -> path.getFileName().toString().startsWith("alias_consumer-"))
                .findFirst().orElseThrow();
        try (JarFile jar = new JarFile(consumerOutput.toFile())) {
            String modsToml = new String(jar.getInputStream(jar.getJarEntry("META-INF/mods.toml"))
                    .readAllBytes(), StandardCharsets.UTF_8);
            String originalMetadata = new String(jar.getInputStream(jar.getJarEntry("fabric.mod.json"))
                    .readAllBytes(), StandardCharsets.UTF_8);
            String bridgeMetadata = new String(jar.getInputStream(
                    jar.getJarEntry("META-INF/loaderbridge.json")).readAllBytes(),
                    StandardCharsets.UTF_8);
            assertThat(modsToml)
                    .contains("modId=\"canonical_provider\"")
                    .doesNotContain("modId=\"provided_api\"");
            assertThat(originalMetadata).contains("\"provided_api\":\">=1.0.0\"");
            assertThat(bridgeMetadata).contains(
                    "\"provided_api\": \"canonical_provider\"");
        }
    }

    @Test
    void refusesToPrepareAmbiguousFabricAliasProviders() throws Exception {
        Path first = temporaryDirectory.resolve("first-alias-provider.jar");
        Files.write(first, jarBytes("""
                {"schemaVersion":1,"id":"first_provider","version":"1.0.0",
                 "provides":["shared_api"]}
                """));
        Path second = temporaryDirectory.resolve("second-alias-provider.jar");
        Files.write(second, jarBytes("""
                {"schemaVersion":1,"id":"second_provider","version":"1.0.0",
                 "provides":["shared_api"]}
                """));
        BridgeRequest request = new BridgeRequest("1.21.1", new LoaderId("forge"), "52.1.0",
                BridgeEnvironment.SERVER, List.of(first, second),
                temporaryDirectory.resolve("ambiguous-alias-output"),
                temporaryDirectory.resolve("ambiguous-alias-cache"));
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isFalse();
        assertThat(plan.diagnostics()).anySatisfy(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo("LB-DEPS-006");
            assertThat(diagnostic.message()).contains(
                    "shared_api", "first_provider", "second_provider");
        });
        assertThat(result.artifacts()).isEmpty();
    }

    @Test
    void replacesBundledFabricApiModulesInsteadOfTransformingThem() throws Exception {
        byte[] renderingModule = jarBytes("""
                {"schemaVersion":1,"id":"fabric-rendering-v1","version":"5.1.0"}
                """);
        byte[] transitiveAccessWideners = jarBytes("""
                {"schemaVersion":1,"id":"fabric-transitive-access-wideners-v1","version":"6.2.0"}
                """);
        Path source = temporaryDirectory.resolve("fabric-api.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(source))) {
            jar.putNextEntry(new JarEntry("fabric.mod.json"));
            jar.write("""
                    {"schemaVersion":1,"id":"fabric-api","version":"0.116.15+1.21.1",
                     "jars":[{"file":"META-INF/jars/fabric-rendering-v1.jar"},
                              {"file":"META-INF/jars/fabric-transitive-access-wideners-v1.jar"}]}
                    """.getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("META-INF/jars/fabric-rendering-v1.jar"));
            jar.write(renderingModule);
            jar.closeEntry();
            jar.putNextEntry(new JarEntry(
                    "META-INF/jars/fabric-transitive-access-wideners-v1.jar"));
            jar.write(transitiveAccessWideners);
            jar.closeEntry();
        }
        BridgeRequest request = new BridgeRequest("1.21.1", new LoaderId("forge"), "52.1.0",
                BridgeEnvironment.CLIENT, List.of(source), temporaryDirectory.resolve("output-fapi"),
                temporaryDirectory.resolve("cache-fapi"));
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var result = adapter.prepare(request, adapter.plan(request));

        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .containsExactlyInAnyOrder(
                        "fabric-api-0.116.15_1.21.1-loaderbridge.jar",
                        "fabric-transitive-access-wideners-v1-6.2.0-loaderbridge.jar");
    }

    @Test
    void deduplicatesSameVersionModulesAndSelectsTheHighestNestedVersion() throws Exception {
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
        assertThat(collisionPlan.canPrepare()).isTrue();
        assertThat(collisionPlan.diagnostics()).anySatisfy(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo("LB-NESTED-101");
            assertThat(diagnostic.message()).contains("2.0.0", "2 available nested variants");
        });
        var collisionResult = adapter.prepare(collisionRequest, collisionPlan);
        assertThat(collisionResult.artifacts()).extracting(path -> path.getFileName().toString())
                .containsExactlyInAnyOrder("collision_parent-1-loaderbridge.jar",
                        "fabric_api_module-2.0.0-loaderbridge.jar");
    }

    @Test
    void rejectsDuplicateMandatoryRootMods() throws Exception {
        Path first = temporaryDirectory.resolve("duplicate-root-first.jar");
        Path second = temporaryDirectory.resolve("duplicate-root-second.jar");
        Files.write(first, jarBytes("""
                {"schemaVersion":1,"id":"duplicate_root","version":"1.0.0"}
                """));
        Files.write(second, jarBytes("""
                {"schemaVersion":1,"id":"duplicate_root","version":"2.0.0"}
                """));
        BridgeRequest request = new BridgeRequest("1.21.1", new LoaderId("forge"), "52.1.0",
                BridgeEnvironment.CLIENT, List.of(first, second),
                temporaryDirectory.resolve("output-duplicate-roots"),
                temporaryDirectory.resolve("cache-duplicate-roots"));

        var plan = new FabricToForgeAdapter().plan(request);

        assertThat(plan.canPrepare()).isFalse();
        assertThat(plan.diagnostics()).anySatisfy(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo("LB-NESTED-006");
            assertThat(diagnostic.message()).contains("Duplicate root", "duplicate_root");
        });
    }

    @Test
    void selectsTheHighestNestedVersionThatSatisfiesMandatoryDependencies() throws Exception {
        byte[] first = jarBytes("""
                {"schemaVersion":1,"id":"constrained_library","version":"1.4.0"}
                """);
        byte[] second = jarBytes("""
                {"schemaVersion":1,"id":"constrained_library","version":"2.0.0"}
                """);
        Path parent = nestedParent("constrained_parent",
                ",\"depends\":{\"constrained_library\":\"1.x\"}", first, second);
        BridgeRequest request = new BridgeRequest("1.21.1", new LoaderId("forge"), "52.1.0",
                BridgeEnvironment.CLIENT, List.of(parent),
                temporaryDirectory.resolve("output-constrained-nested"),
                temporaryDirectory.resolve("cache-constrained-nested"));
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(plan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .doesNotContain("LB-DEPS-002");
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .containsExactlyInAnyOrder("constrained_parent-1-loaderbridge.jar",
                        "constrained_library-1.4.0-loaderbridge.jar");
    }

    @Test
    void selectsANestedVariantWhoseOwnDependenciesMatchTheHost() throws Exception {
        byte[] incompatible = jarBytes("""
                {"schemaVersion":1,"id":"host_constrained_library","version":"2.0.0",
                 "depends":{"minecraft":">=1.22"}}
                """);
        byte[] compatible = jarBytes("""
                {"schemaVersion":1,"id":"host_constrained_library","version":"1.5.0",
                 "depends":{"minecraft":"1.21.x"}}
                """);
        Path parent = nestedParent("host_constrained_parent", incompatible, compatible);
        BridgeRequest request = new BridgeRequest("1.21.1", new LoaderId("forge"), "52.1.0",
                BridgeEnvironment.CLIENT, List.of(parent),
                temporaryDirectory.resolve("output-host-constrained-nested"),
                temporaryDirectory.resolve("cache-host-constrained-nested"));
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(plan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .doesNotContain("LB-DEPS-002", "LB-NESTED-007");
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .containsExactlyInAnyOrder("host_constrained_parent-1-loaderbridge.jar",
                        "host_constrained_library-1.5.0-loaderbridge.jar");
    }

    @Test
    void excludesChildrenOfAnUnselectedNestedParentVariant() throws Exception {
        byte[] childTwo = jarBytes("""
                {"schemaVersion":1,"id":"child_from_parent_two","version":"1"}
                """);
        byte[] childOne = jarBytes("""
                {"schemaVersion":1,"id":"child_from_parent_one","version":"1"}
                """);
        byte[] parentTwo = jarBytesWithNested("""
                {"schemaVersion":1,"id":"variant_parent","version":"2",
                 "jars":[{"file":"META-INF/jars/child.jar"}]}
                """, childTwo);
        byte[] parentOne = jarBytesWithNested("""
                {"schemaVersion":1,"id":"variant_parent","version":"1",
                 "jars":[{"file":"META-INF/jars/child.jar"}]}
                """, childOne);
        Path root = nestedParent("parent_reachability_root",
                ",\"depends\":{\"variant_parent\":\"1\"}", parentTwo, parentOne);
        BridgeRequest request = new BridgeRequest("1.21.1", new LoaderId("forge"), "52.1.0",
                BridgeEnvironment.CLIENT, List.of(root),
                temporaryDirectory.resolve("output-parent-reachability"),
                temporaryDirectory.resolve("cache-parent-reachability"));
        FabricToForgeAdapter adapter = new FabricToForgeAdapter();

        var plan = adapter.plan(request);
        var result = adapter.prepare(request, plan);

        assertThat(plan.canPrepare()).isTrue();
        assertThat(result.artifacts()).extracting(path -> path.getFileName().toString())
                .containsExactlyInAnyOrder(
                        "parent_reachability_root-1-loaderbridge.jar",
                        "variant_parent-1-loaderbridge.jar",
                        "child_from_parent_one-1-loaderbridge.jar")
                .doesNotContain("child_from_parent_two-1-loaderbridge.jar");
    }

    private Path nestedParent(String id, byte[] first, byte[] second) throws Exception {
        return nestedParent(id, "", first, second);
    }

    private Path nestedParent(String id, String metadataSuffix, byte[] first, byte[] second)
            throws Exception {
        Path source = temporaryDirectory.resolve(id + ".jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(source))) {
            jar.putNextEntry(new JarEntry("fabric.mod.json"));
            jar.write(("{\"schemaVersion\":1,\"id\":\"" + id + "\",\"version\":\"1\","
                    + "\"jars\":[{\"file\":\"META-INF/jars/first.jar\"},"
                    + "{\"file\":\"META-INF/jars/second.jar\"}]" + metadataSuffix + "}")
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

    private Path fabricLoaderDependentMod(String id, String predicate) throws Exception {
        Path source = temporaryDirectory.resolve(id + ".jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(source))) {
            jar.putNextEntry(new JarEntry("fabric.mod.json"));
            jar.write(("{\"schemaVersion\":1,\"id\":\"" + id
                    + "\",\"version\":\"1\",\"depends\":{\"fabricloader\":\""
                    + predicate + "\"}}")
                    .getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
        return source;
    }

    private Path localVariableMixinMod(String id, String fabricLoaderPredicate) throws Exception {
        Path source = temporaryDirectory.resolve(id + ".jar");
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "fixture/" + id, null,
                "java/lang/Object", null);
        var method = writer.visitMethod(Opcodes.ACC_PRIVATE, "modify", "(I)I", null, null);
        method.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/ModifyVariable;", false)
                .visitEnd();
        method.visitEnd();
        writer.visitEnd();
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(source))) {
            jar.putNextEntry(new JarEntry("fabric.mod.json"));
            jar.write(("{\"schemaVersion\":1,\"id\":\"" + id
                    + "\",\"version\":\"1\",\"mixins\":[\"" + id
                    + ".mixins.json\"],\"depends\":{\"fabricloader\":\""
                    + fabricLoaderPredicate + "\"}}")
                    .getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("fixture/" + id + ".class"));
            jar.write(writer.toByteArray());
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

    private static byte[] jarBytesWithNested(String metadata, byte[] nested) throws Exception {
        var output = new java.io.ByteArrayOutputStream();
        try (JarOutputStream jar = new JarOutputStream(output)) {
            jar.putNextEntry(new JarEntry("fabric.mod.json"));
            jar.write(metadata.getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("META-INF/jars/child.jar"));
            jar.write(nested);
            jar.closeEntry();
        }
        return output.toByteArray();
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
