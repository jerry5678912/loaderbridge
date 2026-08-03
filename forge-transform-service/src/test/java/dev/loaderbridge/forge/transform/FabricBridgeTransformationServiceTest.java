package dev.loaderbridge.forge.transform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FabricBridgeTransformationServiceTest {
    @TempDir Path temporaryDirectory;

    @Test
    void mergesVersionTwoAndTransitiveRulesFromDeterministicallyOrderedMods() throws Exception {
        Path mods = Files.createDirectories(temporaryDirectory.resolve("mods"));
        writeWidenerJar(mods.resolve("b.jar"), "b.accesswidener", """
                accessWidener v2 official
                transitive-mutable field net/minecraft/Example value I
                """);
        writeWidenerJar(mods.resolve("a.jar"), "a.accesswidener", """
                accessWidener v2 official
                accessible class net/minecraft/Example
                """);

        var merged = FabricBridgeTransformationService.loadAccessWideners(mods);

        assertThat(merged.getTargets()).contains("net.minecraft.Example");
    }

    @Test
    void rejectsMalformedWidenersWithStableDiagnostic() throws Exception {
        Path mods = Files.createDirectories(temporaryDirectory.resolve("malformed-mods"));
        writeWidenerJar(mods.resolve("bad.jar"), "bad.accesswidener", "not an access widener\n");

        assertThatThrownBy(() -> FabricBridgeTransformationService.loadAccessWideners(mods))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("LB-AW-015")
                .hasMessageContaining("bad.jar");
    }

    @Test
    void rejectsUnsafeRegisteredResourcePaths() throws Exception {
        Path mods = Files.createDirectories(temporaryDirectory.resolve("unsafe-mods"));
        writeWidenerJar(mods.resolve("unsafe.jar"), "../escape.accesswidener", """
                accessWidener v2 official
                accessible class net/minecraft/Example
                """);

        assertThatThrownBy(() -> FabricBridgeTransformationService.loadAccessWideners(mods))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("LB-AW-016");
    }

    private static void writeWidenerJar(Path output, String resource, String content)
            throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("LoaderBridge-Access-Widener", resource);
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(output), manifest)) {
            JarEntry entry = new JarEntry(resource);
            jar.putNextEntry(entry);
            jar.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            jar.closeEntry();
        }
    }
}
