package dev.loaderbridge.catalog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class CatalogDependencyLockCodec {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public byte[] encode(CatalogSnapshot snapshot, ResolvedDependencyGraph graph) {
        JsonObject document = new JsonObject();
        document.addProperty("schemaVersion", 1);
        document.addProperty("snapshotId", snapshot.snapshotId());
        document.addProperty("snapshotSha256", sha256(new CatalogSnapshotCodec().encode(snapshot)));
        document.addProperty("minecraftVersion", snapshot.minecraftVersion());
        document.addProperty("loader", snapshot.loader());
        JsonArray roots = new JsonArray();
        snapshot.entries().forEach(entry -> roots.add(entry.project().repository().value()
                + ":" + entry.artifact().versionId()));
        document.add("roots", roots);
        JsonArray artifacts = new JsonArray();
        graph.installationOrder().forEach(artifact ->
                artifacts.add(RepositoryResolutionLockCodec.artifact(artifact)));
        document.add("artifacts", artifacts);
        document.add("resolvedEdges", RepositoryResolutionLockCodec.resolvedEdges(graph));
        return (JSON.toJson(document) + "\n").getBytes(StandardCharsets.UTF_8);
    }

    public void write(CatalogSnapshot snapshot, ResolvedDependencyGraph graph, Path destination)
            throws IOException {
        Path absolute = destination.toAbsolutePath().normalize();
        Path parent = absolute.getParent();
        if (parent == null) {
            throw new IOException("Catalog dependency lock needs a parent directory");
        }
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, absolute.getFileName().toString(), ".tmp");
        try {
            Files.write(temporary, encode(snapshot, graph));
            try {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
