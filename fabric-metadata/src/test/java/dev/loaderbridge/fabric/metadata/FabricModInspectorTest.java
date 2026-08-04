package dev.loaderbridge.fabric.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class FabricModInspectorTest {
    @TempDir
    Path tempDirectory;

    @Test
    void defaultLimitsAllowResourceHeavyContentMods() {
        assertThat(JarReadLimits.DEFAULT.maxEntries()).isGreaterThanOrEqualTo(20_000);
        assertThat(JarReadLimits.DEFAULT.maxEntryBytes()).isEqualTo(64L << 20);
        assertThat(JarReadLimits.DEFAULT.maxTotalBytes()).isEqualTo(512L << 20);
    }

    @Test
    void parsesFabricMetadataFromJarContentsRatherThanFilename() throws IOException {
        Path jar = tempDirectory.resolve("definitely-not-fabric.bin");
        writeJar(jar, Map.of(
                "fabric.mod.json", """
                        {
                          "schemaVersion": 1,
                          "id": "example_mod",
                          "version": "1.2.3",
                          "name": "Example",
                          "description": "Metadata fixture",
                          "authors": ["Jerry", {"name":"Fabric Team","contact":{"homepage":"https://fabricmc.net"}}],
                          "contributors": [{"name":"Contributor"}],
                          "contact": {"sources":"https://example.invalid/source"},
                          "license": ["Apache-2.0", "MIT"],
                          "icon": {"32":"assets/icon-32.png", "128":"assets/icon-128.png"},
                          "custom": {"flag":true,"count":3,"nested":{"value":"yes"}},
                          "environment": "*",
                          "entrypoints": {
                            "main": ["example.Main", {"adapter":"custom", "value":"example.Other"}],
                            "client": ["example.Client"]
                          },
                          "depends": {"fabricloader": ">=0.16.0", "minecraft": "~1.21.1"},
                          "breaks": {"bad_mod": "*"},
                          "provides": ["example_alias"],
                          "mixins": ["example.mixins.json", {"config":"client.mixins.json", "environment":"client"}],
                          "accessWidener": "example.accesswidener",
                          "jars": [{"file":"META-INF/jars/nested.jar"}],
                          "languageAdapters": {"custom":"example.CustomAdapter"}
                        }
                        """,
                "META-INF/jars/nested.jar", jarBytes("""
                        {"schemaVersion":1,"id":"nested_mod","version":"1.0.0"}
                        """)));

        FabricModTree tree = new FabricModInspector().inspect(jar);

        FabricModMetadata root = tree.root();
        assertThat(root.id()).isEqualTo("example_mod");
        assertThat(root.entrypoints().get("main"))
                .extracting(FabricEntrypoint::value)
                .containsExactly("example.Main", "example.Other");
        assertThat(root.dependencies().depends()).containsEntry("fabricloader", java.util.List.of(">=0.16.0"));
        assertThat(root.dependencies().breaks()).containsKey("bad_mod");
        assertThat(root.provides()).containsExactly("example_alias");
        assertThat(root.mixins()).extracting(FabricMixin::config)
                .containsExactly("example.mixins.json", "client.mixins.json");
        assertThat(root.accessWidener()).contains("example.accesswidener");
        assertThat(root.languageAdapters()).containsEntry("custom", "example.CustomAdapter");
        assertThat(root.description()).isEqualTo("Metadata fixture");
        assertThat(root.authors()).extracting(FabricPerson::name)
                .containsExactly("Jerry", "Fabric Team");
        assertThat(root.authors().get(1).contact()).containsEntry("homepage", "https://fabricmc.net");
        assertThat(root.contributors()).extracting(FabricPerson::name).containsExactly("Contributor");
        assertThat(root.contact()).containsEntry("sources", "https://example.invalid/source");
        assertThat(root.licenses()).containsExactly("Apache-2.0", "MIT");
        assertThat(root.icons()).containsEntry(32, "assets/icon-32.png")
                .containsEntry(128, "assets/icon-128.png");
        assertThat(root.customJson()).containsEntry("flag", "true")
                .containsEntry("count", "3");
        assertThat(tree.nested()).extracting(child -> child.root().id()).containsExactly("nested_mod");
    }

    @Test
    void rejectsTraversalEntriesEvenThoughInspectionDoesNotExtractFiles() throws IOException {
        Path jar = tempDirectory.resolve("traversal.jar");
        writeJar(jar, Map.of(
                "fabric.mod.json", """
                        {"schemaVersion":1,"id":"safe_mod","version":"1.0.0"}
                        """,
                "../outside.class", "bad"));

        assertThatThrownBy(() -> new FabricModInspector().inspect(jar))
                .isInstanceOf(UnsafeJarException.class)
                .hasMessageContaining("unsafe entry path");
    }

    @Test
    void rejectsArchivesThatExceedConfiguredEntryLimit() throws IOException {
        Path jar = tempDirectory.resolve("too-many.jar");
        writeJar(jar, Map.of(
                "fabric.mod.json", """
                        {"schemaVersion":1,"id":"safe_mod","version":"1.0.0"}
                        """,
                "one.txt", "1",
                "two.txt", "2"));

        FabricModInspector inspector = new FabricModInspector(new JarReadLimits(2, 1024, 4096, 2));

        assertThatThrownBy(() -> inspector.inspect(jar))
                .isInstanceOf(UnsafeJarException.class)
                .hasMessageContaining("entry limit");
    }

    @Test
    void normalizesCaseInsensitiveModAndMixinEnvironments() throws IOException {
        Path jar = tempDirectory.resolve("environment-case.jar");
        writeJar(jar, Map.of("fabric.mod.json", """
                {"schemaVersion":1,"id":"case_mod","version":"1.0.0",
                 "environment":"CLIENT",
                 "mixins":[{"config":"server.mixins.json","environment":"SERVER"}]}
                """));

        FabricModMetadata metadata = new FabricModInspector().inspect(jar).root();

        assertThat(metadata.environment()).isEqualTo("client");
        assertThat(metadata.mixins()).singleElement()
                .extracting(FabricMixin::environment).isEqualTo("server");
    }

    @Test
    void rejectsUnknownModAndMixinEnvironments() throws IOException {
        Path invalidMod = tempDirectory.resolve("invalid-mod-environment.jar");
        writeJar(invalidMod, Map.of("fabric.mod.json", """
                {"schemaVersion":1,"id":"invalid_mod","version":"1.0.0",
                 "environment":"desktop"}
                """));
        Path invalidMixin = tempDirectory.resolve("invalid-mixin-environment.jar");
        writeJar(invalidMixin, Map.of("fabric.mod.json", """
                {"schemaVersion":1,"id":"invalid_mixin","version":"1.0.0",
                 "mixins":[{"config":"invalid.mixins.json","environment":"desktop"}]}
                """));

        assertThatThrownBy(() -> new FabricModInspector().inspect(invalidMod))
                .isInstanceOf(UnsafeJarException.class)
                .hasMessageContaining("Invalid environment type: desktop");
        assertThatThrownBy(() -> new FabricModInspector().inspect(invalidMixin))
                .isInstanceOf(UnsafeJarException.class)
                .hasMessageContaining("Invalid environment type: desktop");
    }

    @Test
    void enforcesFabricProvidesShapeTypesAndIdentifierRules() throws IOException {
        Path nonArray = tempDirectory.resolve("provides-not-array.jar");
        writeJar(nonArray, Map.of("fabric.mod.json", """
                {"schemaVersion":1,"id":"shape_mod","version":"1.0.0",
                 "provides":"alias"}
                """));
        Path nonString = tempDirectory.resolve("provides-not-string.jar");
        writeJar(nonString, Map.of("fabric.mod.json", """
                {"schemaVersion":1,"id":"type_mod","version":"1.0.0",
                 "provides":[42]}
                """));
        Path invalidId = tempDirectory.resolve("provides-invalid-id.jar");
        writeJar(invalidId, Map.of("fabric.mod.json", """
                {"schemaVersion":1,"id":"id_mod","version":"1.0.0",
                 "provides":["Invalid Alias"]}
                """));

        FabricModInspector inspector = new FabricModInspector();

        assertThatThrownBy(() -> inspector.inspect(nonArray))
                .isInstanceOf(UnsafeJarException.class)
                .hasMessageContaining("Provides must be an array");
        assertThatThrownBy(() -> inspector.inspect(nonString))
                .isInstanceOf(UnsafeJarException.class)
                .hasMessageContaining("Provided id must be a string");
        assertThatThrownBy(() -> inspector.inspect(invalidId))
                .isInstanceOf(UnsafeJarException.class)
                .hasMessageContaining("Invalid Fabric provides declaration");
    }

    private static byte[] jarBytes(String metadata) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            put(zip, "fabric.mod.json", metadata.getBytes(StandardCharsets.UTF_8));
        }
        return output.toByteArray();
    }

    private static void writeJar(Path path, Map<String, ?> entries) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(path))) {
            for (Map.Entry<String, ?> entry : entries.entrySet()) {
                byte[] value = entry.getValue() instanceof byte[] bytes
                        ? bytes
                        : entry.getValue().toString().getBytes(StandardCharsets.UTF_8);
                put(zip, entry.getKey(), value);
            }
        }
    }

    private static void put(ZipOutputStream zip, String name, byte[] value) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(0L);
        zip.putNextEntry(entry);
        zip.write(value);
        zip.closeEntry();
    }
}
