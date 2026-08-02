package dev.loaderbridge.catalog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.loaderbridge.api.repository.RepositoryArtifact;
import dev.loaderbridge.api.repository.RepositoryDependency;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

public final class CatalogSnapshotCodec {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public byte[] encode(CatalogSnapshot snapshot) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", snapshot.schemaVersion());
        root.addProperty("snapshotId", snapshot.snapshotId());
        root.addProperty("frozenAt", snapshot.frozenAt().toString());
        root.addProperty("minecraftVersion", snapshot.minecraftVersion());
        root.addProperty("loader", snapshot.loader());
        JsonArray entries = new JsonArray();
        snapshot.entries().forEach(entry -> entries.add(entry(entry)));
        root.add("entries", entries);
        return (JSON.toJson(root) + "\n").getBytes(StandardCharsets.UTF_8);
    }

    public void write(CatalogSnapshot snapshot, Path destination) throws IOException {
        Path absolute = destination.toAbsolutePath().normalize();
        Path parent = absolute.getParent();
        if (parent == null) {
            throw new IOException("Catalog destination needs a parent directory");
        }
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, absolute.getFileName().toString(), ".tmp");
        try {
            Files.write(temporary, encode(snapshot));
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

    private static JsonObject entry(CatalogEntry entry) {
        JsonObject value = new JsonObject();
        value.addProperty("rank", entry.rank());
        JsonObject project = new JsonObject();
        project.addProperty("repository", entry.project().repository().value());
        project.addProperty("projectId", entry.project().projectId());
        project.addProperty("slug", entry.project().slug());
        project.addProperty("title", entry.project().title());
        project.addProperty("downloads", entry.project().downloads());
        entry.project().sourceUrl().ifPresent(uri -> project.addProperty("sourceUrl", uri.toString()));
        value.add("project", project);
        value.add("artifact", artifact(entry.artifact()));
        return value;
    }

    private static JsonObject artifact(RepositoryArtifact artifact) {
        JsonObject value = new JsonObject();
        value.addProperty("versionId", artifact.versionId());
        value.addProperty("versionNumber", artifact.versionNumber());
        value.addProperty("fileName", artifact.fileName());
        value.addProperty("downloadUrl", artifact.downloadUrl().toString());
        value.addProperty("size", artifact.size());
        JsonObject hashes = new JsonObject();
        artifact.hashes().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
                .forEach(hash -> hashes.addProperty(hash.getKey().name().toLowerCase(
                        java.util.Locale.ROOT), hash.getValue()));
        value.add("hashes", hashes);
        value.addProperty("publishedAt", artifact.publishedAt().toString());
        value.addProperty("releaseChannel", artifact.releaseChannel().name().toLowerCase(
                java.util.Locale.ROOT));
        value.add("gameVersions", strings(artifact.gameVersions()));
        value.add("loaders", strings(artifact.loaders()));
        JsonArray dependencies = new JsonArray();
        artifact.dependencies().stream().sorted(Comparator
                .comparing((RepositoryDependency dependency) -> nullSafe(dependency.projectId()))
                .thenComparing(dependency -> nullSafe(dependency.versionId()))
                .thenComparing(dependency -> dependency.kind().name())).forEach(dependency -> {
                    JsonObject item = new JsonObject();
                    if (dependency.projectId() != null) {
                        item.addProperty("projectId", dependency.projectId());
                    }
                    if (dependency.versionId() != null) {
                        item.addProperty("versionId", dependency.versionId());
                    }
                    item.addProperty("kind", dependency.kind().name().toLowerCase(java.util.Locale.ROOT));
                    dependencies.add(item);
                });
        value.add("dependencies", dependencies);
        return value;
    }

    private static JsonArray strings(java.util.Set<String> source) {
        JsonArray values = new JsonArray();
        source.stream().sorted().forEach(values::add);
        return values;
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
