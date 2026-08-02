package dev.loaderbridge.fabric.remap;

import static org.assertj.core.api.Assertions.assertThat;

import dev.loaderbridge.api.BridgeAdapter;
import dev.loaderbridge.api.BridgeEnvironment;
import dev.loaderbridge.api.BridgeRequest;
import dev.loaderbridge.api.LoaderId;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
                .contains("sourceSha256", "outputSha256");
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
                .contains("LB-MIXIN-001", "LB-AW-001")
                .doesNotContain("LB-NESTED-001");
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
    }

    private static byte[] jarBytes(String metadata) throws Exception {
        var output = new java.io.ByteArrayOutputStream();
        try (JarOutputStream jar = new JarOutputStream(output)) {
            jar.putNextEntry(new JarEntry("fabric.mod.json"));
            jar.write(metadata.getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
        return output.toByteArray();
    }
}
