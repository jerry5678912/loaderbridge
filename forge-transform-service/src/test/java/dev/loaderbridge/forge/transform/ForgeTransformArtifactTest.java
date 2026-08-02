package dev.loaderbridge.forge.transform;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;

class ForgeTransformArtifactTest {
    @Test
    void isAStandaloneModLauncherTransformationService() throws Exception {
        Path transformJar = Path.of(System.getProperty("loaderbridge.transformJar"));

        try (JarFile jar = new JarFile(transformJar.toFile())) {
            assertThat(jar.getEntry("META-INF/services/cpw.mods.modlauncher.api.ITransformationService"))
                    .isNotNull();
            assertThat(jar.getEntry("META-INF/services/net.minecraftforge.forgespi.language.IModLanguageProvider"))
                    .isNull();
        }
    }
}
