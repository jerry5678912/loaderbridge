package dev.loaderbridge.fabric.remap;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class MinecraftArtifactResolverLiveTest {
    @Test
    @EnabledIfEnvironmentVariable(named = "LOADERBRIDGE_LIVE", matches = "true")
    void resolvesOfficialMinecraft1211ArtifactsFromMojang() throws Exception {
        Path cache = Path.of("build/live-minecraft-cache");

        ResolvedMinecraftArtifacts resolved = new MinecraftArtifactResolver()
                .resolve("1.21.1", cache, true);

        assertThat(resolved.clientJar().sha1()).hasSize(40);
        assertThat(resolved.clientMappings().sha1()).hasSize(40);
        assertThat(Files.size(resolved.clientJar().path())).isEqualTo(resolved.clientJar().size());
        assertThat(Files.size(resolved.clientMappings().path())).isEqualTo(resolved.clientMappings().size());
        try (JarFile client = new JarFile(resolved.clientJar().path().toFile())) {
            assertThat(client.size()).isGreaterThan(1_000);
        }
    }
}
