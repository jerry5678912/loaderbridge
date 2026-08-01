package dev.loaderbridge.fabric.remap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;

/** Thin deterministic wrapper around TinyRemapper; mapping resolution stays outside this class. */
public final class TinyRemapperEngine {
    public void remap(Path input, Path output, Path mappings, String sourceNamespace,
            String targetNamespace, List<Path> classpath) throws IOException {
        IMappingProvider mappingProvider = TinyUtils.createTinyMappingProvider(
                mappings, sourceNamespace, targetNamespace);
        TinyRemapper remapper = TinyRemapper.newRemapper()
                .withMappings(mappingProvider)
                .renameInvalidLocals(true)
                .rebuildSourceFilenames(true)
                .build();
        Files.createDirectories(output.toAbsolutePath().getParent());
        try (OutputConsumerPath consumer = new OutputConsumerPath.Builder(output).build()) {
            consumer.addNonClassFiles(input, NonClassCopyMode.UNCHANGED, remapper);
            if (!classpath.isEmpty()) {
                remapper.readClassPath(classpath.toArray(Path[]::new));
            }
            remapper.readInputs(input);
            remapper.apply(consumer);
        } finally {
            remapper.finish();
        }
    }
}
