package dev.loaderbridge.fabric.remap;

import static org.assertj.core.api.Assertions.assertThat;

import dev.loaderbridge.api.BridgeAdapter;
import dev.loaderbridge.api.BridgeCapability;
import dev.loaderbridge.api.BridgeEnvironment;
import dev.loaderbridge.api.BridgeRequest;
import dev.loaderbridge.api.DiagnosticSeverity;
import dev.loaderbridge.api.LoaderId;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;
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
                .contains("sourceSha256", "outputSha256", "\"adapterVersion\": \"0.3.6\"",
                        "adapterArtifactSha256");
        try (JarFile jar = new JarFile(result.artifacts().getFirst().toFile())) {
            assertThat(jar.getEntry("pack.mcmeta")).isNotNull();
            assertThat(new String(jar.getInputStream(jar.getJarEntry("META-INF/loaderbridge.json"))
                    .readAllBytes(), StandardCharsets.UTF_8))
                    .contains("\"sourceNamespace\": \"neutral\"");
        }
    }

    @Test
    void gatesCapabilitiesThatAreInventoriedButNotImplemented() throws Exception {
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

        assertThat(plan.canPrepare()).isFalse();
        assertThat(plan.diagnostics()).extracting(diagnostic -> diagnostic.code())
                .contains("LB-AW-001")
                .doesNotContain("LB-MIXIN-001")
                .doesNotContain("LB-NESTED-001");
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
