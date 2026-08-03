package dev.loaderbridge.forge.transform;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import net.fabricmc.accesswidener.AccessWidener;
import net.fabricmc.accesswidener.AccessWidenerReader;

public final class FabricBridgeTransformationService implements ITransformationService {
    private static final long MAXIMUM_ACCESS_WIDENER_BYTES = 4L << 20;
    private static final int MAXIMUM_MOD_JARS = 10_000;
    @SuppressWarnings("rawtypes")
    private List<ITransformer> transformers = List.of();

    @Override
    public String name() {
        return "fabricbridge";
    }

    @Override
    public void initialize(IEnvironment environment) {
        Path gameDirectory = environment.getProperty(IEnvironment.Keys.GAMEDIR.get())
                .orElseThrow(() -> new IllegalStateException("LB-AW-010: game directory is unavailable"));
        try {
            AccessWidener accessWidener = loadAccessWideners(gameDirectory.resolve("mods"));
            transformers = accessWidener.getTargets().isEmpty()
                    ? List.of() : List.of(new AccessWidenerTransformer(accessWidener));
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("LB-AW-011: could not load access wideners", exception);
        }
    }

    @Override
    public void onLoad(IEnvironment environment, Set<String> otherServices) {
        // No incompatible peer transformation service is known at scaffold stage.
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<ITransformer> transformers() {
        return transformers;
    }

    static AccessWidener loadAccessWideners(Path modsDirectory) throws IOException {
        AccessWidener merged = new AccessWidener();
        if (!Files.isDirectory(modsDirectory)) {
            return merged;
        }
        List<Path> jars;
        try (var files = Files.list(modsDirectory)) {
            jars = files.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .limit(MAXIMUM_MOD_JARS + 1L)
                    .toList();
        }
        if (jars.size() > MAXIMUM_MOD_JARS) {
            throw new IOException("LB-AW-012: mod JAR count exceeds safety limit");
        }
        for (Path jarPath : jars) {
            try (JarFile jar = new JarFile(jarPath.toFile(), false)) {
                if (jar.getManifest() == null) continue;
                String resource = jar.getManifest().getMainAttributes()
                        .getValue("LoaderBridge-Access-Widener");
                if (resource == null) continue;
                validateResource(resource);
                var entry = jar.getJarEntry(resource);
                if (entry == null || entry.isDirectory()) {
                    throw new IOException("LB-AW-013: registered access widener is missing in "
                            + jarPath.getFileName());
                }
                if (entry.getSize() > MAXIMUM_ACCESS_WIDENER_BYTES) {
                    throw new IOException("LB-AW-014: access widener exceeds safety limit");
                }
                byte[] bytes;
                try (InputStream input = jar.getInputStream(entry)) {
                    bytes = input.readNBytes(Math.toIntExact(MAXIMUM_ACCESS_WIDENER_BYTES + 1));
                }
                if (bytes.length > MAXIMUM_ACCESS_WIDENER_BYTES) {
                    throw new IOException("LB-AW-014: access widener exceeds safety limit");
                }
                try {
                    new AccessWidenerReader(merged).read(bytes, "official");
                } catch (RuntimeException exception) {
                    throw new IOException("LB-AW-015: invalid runtime access widener in "
                            + jarPath.getFileName(), exception);
                }
            }
        }
        return merged;
    }

    private static void validateResource(String resource) throws IOException {
        if (resource.isBlank() || resource.startsWith("/") || resource.contains("\\")
                || resource.contains("\r") || resource.contains("\n")
                || List.of(resource.split("/")).contains("..")) {
            throw new IOException("LB-AW-016: unsafe access-widener resource path");
        }
    }
}
