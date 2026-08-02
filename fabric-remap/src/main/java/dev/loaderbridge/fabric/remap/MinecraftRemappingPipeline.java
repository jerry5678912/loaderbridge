package dev.loaderbridge.fabric.remap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/** Builds the intermediary hierarchy classpath before translating a Fabric mod to official names. */
public final class MinecraftRemappingPipeline {
    private final TinyRemapperEngine remapper = new TinyRemapperEngine();
    private final OfficialMappingComposer composer = new OfficialMappingComposer();

    public Path remap(Path inputMod, Path outputMod, Path obfuscatedClientJar,
            Path intermediaryMappings, Path mojangMappings, Path workDirectory) throws IOException {
        Files.createDirectories(workDirectory);
        String minecraftKey = digest(obfuscatedClientJar, intermediaryMappings);
        Path intermediaryClient = workDirectory.resolve("client-intermediary-" + minecraftKey + ".jar");
        if (!Files.isRegularFile(intermediaryClient)) {
            remapper.remap(obfuscatedClientJar, intermediaryClient, intermediaryMappings,
                    "official", "intermediary", List.of());
        }

        Path composedMappings = composeMappings(intermediaryMappings, mojangMappings, workDirectory);

        String modKey = digest(inputMod, composedMappings, intermediaryClient);
        Path remappedMod = workDirectory.resolve("mod-named-" + modKey + ".jar");
        if (!Files.isRegularFile(remappedMod)) {
            remapper.remap(inputMod, remappedMod, composedMappings,
                    "intermediary", "named", List.of(intermediaryClient));
        }
        Files.createDirectories(outputMod.toAbsolutePath().getParent());
        Files.copy(remappedMod, outputMod, StandardCopyOption.REPLACE_EXISTING);
        return composedMappings;
    }

    public Path composeMappings(Path intermediaryMappings, Path mojangMappings,
            Path workDirectory) throws IOException {
        Files.createDirectories(workDirectory);
        String mappingKey = digest(intermediaryMappings, mojangMappings);
        Path composedMappings = workDirectory.resolve("intermediary-named-" + mappingKey + ".tiny");
        if (!Files.isRegularFile(composedMappings)) {
            composer.compose(intermediaryMappings, mojangMappings, composedMappings);
        }
        return composedMappings;
    }

    private static String digest(Path... paths) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Path path : paths) {
                digest.update(Files.readAllBytes(path));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by Java", exception);
        }
    }
}
