package dev.loaderbridge.fabric.remap;

import static org.assertj.core.api.Assertions.assertThat;

import dev.loaderbridge.fabric.metadata.FabricModInspector;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeterministicJarPreparerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void producesStableUnsignedJarWithForgeAndBridgeMetadata() throws Exception {
        Path source = temporaryDirectory.resolve("source.jar");
        writeJar(source, Map.of(
                "fabric.mod.json", "{\"schemaVersion\":1,\"id\":\"fixture\",\"version\":\"1.0.0\"}",
                "example/data.txt", "preserved",
                "META-INF/OLD.SF", "invalid after transform",
                "META-INF/OLD.RSA", "invalid after transform"));

        var metadata = new FabricModInspector().inspect(source).root();
        DeterministicJarPreparer preparer = new DeterministicJarPreparer();
        Path first = temporaryDirectory.resolve("first.jar");
        Path second = temporaryDirectory.resolve("second.jar");

        preparer.prepare(source, first, metadata, PreparationManifest.pinned("1.21.1", "52.1.0"));
        preparer.prepare(source, second, metadata, PreparationManifest.pinned("1.21.1", "52.1.0"));

        assertThat(Files.readAllBytes(first)).isEqualTo(Files.readAllBytes(second));
        try (JarFile jar = new JarFile(first.toFile())) {
            assertThat(jar.getEntry("META-INF/OLD.SF")).isNull();
            assertThat(jar.getEntry("META-INF/OLD.RSA")).isNull();
            assertThat(read(jar, "example/data.txt")).isEqualTo("preserved");
            assertThat(read(jar, "fabric.mod.json")).contains("fixture");
            assertThat(read(jar, "META-INF/mods.toml"))
                    .contains("modLoader=\"fabricbridge\"")
                    .contains("modId=\"fixture\"");
            assertThat(read(jar, "META-INF/loaderbridge.json"))
                    .contains("\"sourceLoader\": \"fabric\"")
                    .contains("\"minecraftVersion\": \"1.21.1\"");
        }
    }

    private static String read(JarFile jar, String name) throws IOException {
        try (var input = jar.getInputStream(jar.getJarEntry(name))) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void writeJar(Path output, Map<String, String> entries) throws IOException {
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(output))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                JarEntry jarEntry = new JarEntry(entry.getKey());
                jarEntry.setTime(System.currentTimeMillis());
                jar.putNextEntry(jarEntry);
                jar.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                jar.closeEntry();
            }
        }
    }
}
