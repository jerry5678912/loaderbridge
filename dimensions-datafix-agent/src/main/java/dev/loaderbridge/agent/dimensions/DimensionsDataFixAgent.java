package dev.loaderbridge.agent.dimensions;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.jar.JarFile;

/** Installs the two DFU transforms before Forge constructs its secure module layers. */
public final class DimensionsDataFixAgent {
    private static final int MAXIMUM_MOD_JARS = 10_000;

    private DimensionsDataFixAgent() { }

    public static void premain(String arguments, Instrumentation instrumentation) {
        Path mods = arguments == null || arguments.isBlank()
                ? Path.of("mods") : Path.of(arguments);
        try {
            if (isEnabled(mods)) {
                instrumentation.addTransformer(new DimensionsDfuTransformer(), false);
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "LB-DIM-AGENT-001: could not inspect LoaderBridge modules", exception);
        }
    }

    static boolean isEnabled(Path modsDirectory) throws IOException {
        if (!Files.isDirectory(modsDirectory)) return false;
        var jars = new java.util.ArrayList<Path>();
        try (var files = Files.list(modsDirectory)) {
            files.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .limit(MAXIMUM_MOD_JARS + 1L)
                    .forEach(jars::add);
        }
        if (jars.size() > MAXIMUM_MOD_JARS) {
            throw new IOException("LB-DIM-AGENT-002: mod JAR count exceeds safety limit");
        }
        for (Path jarPath : jars) {
            try (JarFile jar = new JarFile(jarPath.toFile(), false)) {
                if (jar.getManifest() != null && Boolean.parseBoolean(jar.getManifest()
                        .getMainAttributes().getValue("LoaderBridge-Dimensions-DataFix"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
