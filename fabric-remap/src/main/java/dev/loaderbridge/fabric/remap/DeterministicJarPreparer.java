package dev.loaderbridge.fabric.remap;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.loaderbridge.fabric.metadata.FabricModMetadata;
import dev.loaderbridge.fabric.metadata.UnsafeJarException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/** Produces the deterministic packaging envelope around transformed bytecode. */
public final class DeterministicJarPreparer {
    private static final long FIXED_ENTRY_TIME = 0L;

    public void prepare(
            Path source,
            Path destination,
            FabricModMetadata metadata,
            PreparationManifest manifest) throws IOException {
        Path parent = destination.toAbsolutePath().getParent();
        if (parent == null) {
            throw new IOException("Output JAR must have a parent directory");
        }
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, destination.getFileName().toString(), ".tmp");
        try {
            write(source, temporary, metadata, manifest);
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | RuntimeException exception) {
            Files.deleteIfExists(temporary);
            throw exception;
        }
    }

    private static void write(
            Path source,
            Path destination,
            FabricModMetadata metadata,
            PreparationManifest manifest) throws IOException {
        try (JarFile input = new JarFile(source.toFile(), false);
                JarOutputStream output = new JarOutputStream(Files.newOutputStream(destination))) {
            List<JarEntry> entries = input.stream()
                    .filter(entry -> !entry.isDirectory())
                    .filter(entry -> !isInvalidatedSignature(entry.getName()))
                    .filter(entry -> !isGeneratedEntry(entry.getName()))
                    .sorted(Comparator.comparing(JarEntry::getName))
                    .toList();
            Set<String> names = new HashSet<>();
            for (JarEntry entry : entries) {
                validateName(entry.getName());
                if (!names.add(entry.getName())) {
                    throw new UnsafeJarException("Duplicate JAR entry: " + entry.getName());
                }
                try (InputStream bytes = input.getInputStream(entry)) {
                    put(output, entry.getName(), readBounded(bytes));
                }
            }
            put(output, "META-INF/loaderbridge.json", bridgeMetadata(metadata, manifest));
            put(output, "META-INF/mods.toml", forgeMetadata(metadata));
        }
    }

    private static byte[] bridgeMetadata(FabricModMetadata metadata, PreparationManifest manifest) {
        JsonObject root = new JsonObject();
        root.addProperty("formatVersion", manifest.formatVersion());
        root.addProperty("adapter", "fabric-to-forge");
        root.addProperty("adapterVersion", manifest.adapterVersion());
        root.addProperty("sourceLoader", "fabric");
        root.addProperty("targetLoader", "forge");
        root.addProperty("minecraftVersion", manifest.minecraftVersion());
        root.addProperty("forgeVersion", manifest.forgeVersion());
        root.addProperty("sourceNamespace", manifest.sourceNamespace());
        root.addProperty("targetNamespace", manifest.targetNamespace());
        root.addProperty("modId", metadata.id());
        root.addProperty("modVersion", metadata.version());
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
                .toJson(root).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] forgeMetadata(FabricModMetadata metadata) {
        String escapedId = toml(metadata.id());
        String escapedVersion = toml(metadata.version());
        String escapedName = toml(metadata.name());
        String text = "modLoader=\"fabricbridge\"\n"
                + "loaderVersion=\"[0.1,1)\"\n"
                + "license=\"LicenseRef-See-fabric.mod.json\"\n\n"
                + "[[mods]]\n"
                + "modId=\"" + escapedId + "\"\n"
                + "version=\"" + escapedVersion + "\"\n"
                + "displayName=\"" + escapedName + "\"\n";
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static String toml(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private static byte[] readBounded(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        long total = 0;
        while ((count = input.read(buffer)) != -1) {
            total += count;
            if (total > (64L << 20)) {
                throw new UnsafeJarException("JAR entry exceeds preprocessing size limit");
            }
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    private static void put(JarOutputStream output, String name, byte[] bytes) throws IOException {
        JarEntry entry = new JarEntry(name);
        entry.setTime(FIXED_ENTRY_TIME);
        output.putNextEntry(entry);
        output.write(bytes);
        output.closeEntry();
    }

    private static boolean isGeneratedEntry(String name) {
        return name.equals("META-INF/mods.toml") || name.equals("META-INF/loaderbridge.json");
    }

    private static boolean isInvalidatedSignature(String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        if (!upper.startsWith("META-INF/")) {
            return false;
        }
        return upper.endsWith(".SF") || upper.endsWith(".RSA") || upper.endsWith(".DSA")
                || upper.endsWith(".EC") || upper.equals("META-INF/INDEX.LIST");
    }

    private static void validateName(String name) throws UnsafeJarException {
        if (name.isBlank() || name.startsWith("/") || name.contains("\\")) {
            throw new UnsafeJarException("Unsafe JAR entry: " + name);
        }
        List<String> parts = new ArrayList<>(List.of(name.split("/")));
        if (parts.contains("..") || parts.contains(".")) {
            throw new UnsafeJarException("Unsafe JAR entry: " + name);
        }
    }
}
