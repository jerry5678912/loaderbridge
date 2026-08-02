package dev.loaderbridge.fabric.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;

class FabricLoaderShimArtifactTest {
    @Test
    void packagesItsRuntimeMetadataDependenciesForForgeGameLayer() throws Exception {
        Path shimJar = Path.of(System.getProperty("loaderbridge.shimJar"));
        try (JarFile jar = new JarFile(shimJar.toFile())) {
            assertThat(jar.getManifest().getMainAttributes().getValue("FMLModType"))
                    .isEqualTo("LIBRARY");
            assertThat(jar.getEntry(
                    "dev/loaderbridge/fabric/metadata/FabricMetadataParser.class")).isNotNull();
            assertThat(jar.getEntry("dev/loaderbridge/api/Diagnostic.class")).isNotNull();
            assertThat(jar.getEntry("net/fabricmc/api/Environment.class")).isNotNull();
            assertThat(jar.getEntry(
                    "net/fabricmc/loader/api/entrypoint/PreLaunchEntrypoint.class")).isNotNull();
        }
    }
}
