package dev.loaderbridge.fabric.metadata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Reads Fabric metadata without loading any class from the inspected artifact. */
public final class FabricModInspector {
    private static final String METADATA_PATH = "fabric.mod.json";
    private final JarReadLimits limits;
    private final FabricMetadataParser parser = new FabricMetadataParser();

    public FabricModInspector() {
        this(JarReadLimits.DEFAULT);
    }

    public FabricModInspector(JarReadLimits limits) {
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    public FabricModTree inspect(Path artifact) throws IOException {
        Objects.requireNonNull(artifact, "artifact");
        try (InputStream input = Files.newInputStream(artifact)) {
            return inspectArchive(input, 0, artifact.toString());
        }
    }

    private FabricModTree inspectArchive(InputStream input, int depth, String source) throws IOException {
        if (depth > limits.maxNestedDepth()) {
            throw new UnsafeJarException("Nested JAR depth limit exceeded in " + source);
        }
        Map<String, byte[]> entries = readEntries(input, source);
        byte[] metadataBytes = entries.get(METADATA_PATH);
        if (metadataBytes == null) {
            throw new UnsafeJarException("Missing fabric.mod.json in " + source);
        }

        FabricModMetadata metadata = parser.parse(metadataBytes);
        List<FabricModTree> nested = new ArrayList<>();
        for (String nestedPath : metadata.nestedJars()) {
            validateEntryPath(nestedPath);
            byte[] nestedBytes = entries.get(nestedPath);
            if (nestedBytes == null) {
                throw new UnsafeJarException("Declared nested JAR is missing: " + nestedPath);
            }
            nested.add(inspectArchive(new ByteArrayInputStream(nestedBytes), depth + 1, source + "!/" + nestedPath));
        }
        return new FabricModTree(metadata, nested);
    }

    private Map<String, byte[]> readEntries(InputStream input, String source) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        long totalBytes = 0;
        int entryCount = 0;
        try (ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                entryCount++;
                if (entryCount > limits.maxEntries()) {
                    throw new UnsafeJarException("JAR entry limit exceeded in " + source);
                }
                String name = entry.getName();
                validateEntryPath(name);
                byte[] bytes = readBounded(zip, name);
                totalBytes += bytes.length;
                if (totalBytes > limits.maxTotalBytes()) {
                    throw new UnsafeJarException("JAR total-size limit exceeded in " + source);
                }
                if (entries.putIfAbsent(name, bytes) != null) {
                    throw new UnsafeJarException("Duplicate JAR entry: " + name);
                }
            }
        }
        return entries;
    }

    private byte[] readBounded(InputStream input, String name) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long read = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
            read += count;
            if (read > limits.maxEntryBytes()) {
                throw new UnsafeJarException("JAR entry-size limit exceeded: " + name);
            }
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    private static void validateEntryPath(String name) throws UnsafeJarException {
        if (name.isBlank() || name.startsWith("/") || name.startsWith("\\") || name.contains("\\")) {
            throw new UnsafeJarException("JAR contains unsafe entry path: " + name);
        }
        Path normalized;
        try {
            normalized = Path.of(name).normalize();
        } catch (RuntimeException exception) {
            throw new UnsafeJarException("JAR contains unsafe entry path: " + name);
        }
        if (normalized.isAbsolute() || normalized.startsWith("..") || !normalized.toString().replace('\\', '/').equals(name)) {
            throw new UnsafeJarException("JAR contains unsafe entry path: " + name);
        }
    }
}
