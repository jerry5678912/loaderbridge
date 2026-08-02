package dev.loaderbridge.forge;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;

class ForgeRuntimeArtifactTest {
    @Test
    void declaresForgeLanguageProviderIdentityAndVersion() throws Exception {
        Path runtimeJar = Path.of(System.getProperty("loaderbridge.runtimeJar"));

        try (JarFile jar = new JarFile(runtimeJar.toFile())) {
            var attributes = jar.getManifest().getMainAttributes();
            assertThat(attributes.getValue("FMLModType")).isEqualTo("LANGPROVIDER");
            assertThat(attributes.getValue("Implementation-Title")).isEqualTo("LoaderBridge Fabric Language");
            assertThat(attributes.getValue("Implementation-Version")).isEqualTo("0.1.0");
            assertThat(jar.getEntry("META-INF/services/net.minecraftforge.forgespi.language.IModLanguageProvider"))
                    .isNotNull();
            assertThat(jar.getEntry("META-INF/services/cpw.mods.modlauncher.api.ITransformationService"))
                    .as("Forge excludes transformation-service JARs from language-provider discovery")
                    .isNull();
        }
    }
}
