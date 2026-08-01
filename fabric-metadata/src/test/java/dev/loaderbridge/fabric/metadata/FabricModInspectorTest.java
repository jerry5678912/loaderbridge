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
    void parsesFabricMetadataFromJarContentsRatherThanFilename() throws IOException {
        Path jar = tempDirectory.resolve("definitely-not-fabric.bin");
        writeJar(jar, Map.of(
                "fabric.mod.json", """
                        {
                          "schemaVersion": 1,
                          "id": "example_mod",
                          "version": "1.2.3",
                          "name": "Example",
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
