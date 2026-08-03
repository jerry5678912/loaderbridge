package dev.loaderbridge.fixture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import org.junit.jupiter.api.Test;

class NestedFixtureArtifactTest {
    @Test
    void embedsACompleteFabricChildJarAtItsDeclaredPath() throws Exception {
        Path fixture = Path.of(System.getProperty("loaderbridge.fixtureJar"));
        try (JarFile parent = new JarFile(fixture.toFile())) {
            var nested = parent.getJarEntry("META-INF/jars/loaderbridge-nested-child.jar");
            assertThat(nested).isNotNull();
            Set<String> entries = new HashSet<>();
            byte[] nestedBytes;
            try (InputStream nestedInput = parent.getInputStream(nested)) {
                nestedBytes = nestedInput.readAllBytes();
            }
            try (var input = new JarInputStream(new ByteArrayInputStream(nestedBytes))) {
                for (var entry = input.getNextJarEntry(); entry != null;
                        entry = input.getNextJarEntry()) {
                    entries.add(entry.getName());
                }
            }
            assertThat(entries).contains(
                    "fabric.mod.json",
                    "dev/loaderbridge/fixture/nested/NestedChildFixture.class");
        }
    }
}
