package dev.loaderbridge.fabric.remap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Extracts the dependency-locked Fabric intermediary mapping resource into the bridge cache. */
public final class BundledIntermediaryMappings implements IntermediaryMappingsProvider {
    @Override
    public Path resolve(String minecraftVersion, Path cacheDirectory) throws IOException {
        if (!minecraftVersion.equals("1.21.1")) {
            throw new ArtifactVerificationException("No bundled intermediary mapping for " + minecraftVersion);
        }
        Path destination = cacheDirectory.resolve("mappings").resolve("intermediary-1.21.1.tiny");
        if (Files.isRegularFile(destination)) {
            return destination;
        }
        Files.createDirectories(destination.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(destination.toAbsolutePath().getParent(), "intermediary-", ".tmp");
        try (InputStream input = BundledIntermediaryMappings.class.getClassLoader()
                .getResourceAsStream("mappings/mappings.tiny")) {
            if (input == null) {
                throw new ArtifactVerificationException(
                        "Dependency-locked Fabric intermediary mapping resource is missing");
            }
            Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            return destination;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
