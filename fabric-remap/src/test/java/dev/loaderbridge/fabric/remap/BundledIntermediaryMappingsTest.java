package dev.loaderbridge.fabric.remap;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.mappingio.MappingReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BundledIntermediaryMappingsTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void extractsDependencyLockedMinecraft1211Mappings() throws Exception {
        Path mappings = new BundledIntermediaryMappings().resolve("1.21.1", temporaryDirectory);

        assertThat(Files.size(mappings)).isGreaterThan(1_000);
        assertThat(MappingReader.getNamespaces(mappings)).containsExactly("official", "intermediary");
    }
}
