package dev.loaderbridge.catalog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.loaderbridge.api.repository.RepositoryArtifact;
import dev.loaderbridge.api.repository.RepositoryDependency;
import dev.loaderbridge.api.repository.DependencyKind;
import dev.loaderbridge.api.repository.HashAlgorithm;
import dev.loaderbridge.api.repository.ReleaseChannel;
import dev.loaderbridge.api.repository.RepositoryId;
import dev.loaderbridge.api.repository.RepositoryProject;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.time.Instant;

public final class CatalogSnapshotCodec {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final long MAXIMUM_SNAPSHOT_BYTES = 32L << 20;
    private static final int MAXIMUM_ENTRIES = 10_000;

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

    public CatalogSnapshot read(Path source) throws IOException {
        if (!Files.isRegularFile(source)) {
            throw new IOException("Catalog snapshot is not a readable file: " + source);
        }
        long size = Files.size(source);
        if (size <= 0 || size > MAXIMUM_SNAPSHOT_BYTES) {
            throw new IOException("Catalog snapshot size is outside the 32 MiB limit");
        }
        try {
            JsonObject root = com.google.gson.JsonParser.parseString(
                    Files.readString(source, StandardCharsets.UTF_8)).getAsJsonObject();
            int schemaVersion = integer(root, "schemaVersion");
            if (schemaVersion != 1) {
                throw new IllegalArgumentException("Unsupported catalog schema version " + schemaVersion);
            }
            JsonArray encodedEntries = array(root, "entries");
            if (encodedEntries.size() > MAXIMUM_ENTRIES) {
                throw new IllegalArgumentException("Catalog snapshot exceeds entry limit");
            }
            List<CatalogEntry> entries = new java.util.ArrayList<>(encodedEntries.size());
            for (var encoded : encodedEntries) {
                JsonObject value = encoded.getAsJsonObject();
                JsonObject projectValue = object(value, "project");
                RepositoryId repository = new RepositoryId(string(projectValue, "repository"));
                String projectId = string(projectValue, "projectId");
                RepositoryProject project = new RepositoryProject(repository, projectId,
                        string(projectValue, "slug"), string(projectValue, "title"),
                        longValue(projectValue, "downloads"), optionalUri(projectValue, "sourceUrl"));
                entries.add(new CatalogEntry(integer(value, "rank"), project,
                        artifact(repository, projectId, object(value, "artifact"))));
            }
            return new CatalogSnapshot(schemaVersion, string(root, "snapshotId"),
                    Instant.parse(string(root, "frozenAt")), string(root, "minecraftVersion"),
                    string(root, "loader"), entries);
        } catch (RuntimeException exception) {
            throw new IOException("Malformed catalog snapshot: " + exception.getMessage(), exception);
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

    private static RepositoryArtifact artifact(RepositoryId repository, String projectId,
            JsonObject value) {
        EnumMap<HashAlgorithm, String> hashes = new EnumMap<>(HashAlgorithm.class);
        object(value, "hashes").entrySet().forEach(entry -> hashes.put(
                HashAlgorithm.valueOf(entry.getKey().toUpperCase(Locale.ROOT)),
                entry.getValue().getAsString()));
        List<RepositoryDependency> dependencies = new java.util.ArrayList<>();
        for (var encoded : array(value, "dependencies")) {
            JsonObject dependency = encoded.getAsJsonObject();
            dependencies.add(new RepositoryDependency(optionalString(dependency, "projectId"),
                    optionalString(dependency, "versionId"),
                    DependencyKind.valueOf(string(dependency, "kind").toUpperCase(Locale.ROOT))));
        }
        return new RepositoryArtifact(repository, projectId, string(value, "versionId"),
                string(value, "versionNumber"), string(value, "fileName"),
                URI.create(string(value, "downloadUrl")), longValue(value, "size"), hashes,
                Instant.parse(string(value, "publishedAt")),
                ReleaseChannel.valueOf(string(value, "releaseChannel").toUpperCase(Locale.ROOT)),
                stringSet(value, "gameVersions"), stringSet(value, "loaders"), dependencies);
    }

    private static JsonArray strings(java.util.Set<String> source) {
        JsonArray values = new JsonArray();
        source.stream().sorted().forEach(values::add);
        return values;
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private static JsonObject object(JsonObject object, String name) {
        return object.getAsJsonObject(name);
    }

    private static JsonArray array(JsonObject object, String name) {
        return object.getAsJsonArray(name);
    }

    private static String string(JsonObject object, String name) {
        return object.get(name).getAsString();
    }

    private static String optionalString(JsonObject object, String name) {
        var value = object.get(name);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    private static int integer(JsonObject object, String name) {
        return object.get(name).getAsInt();
    }

    private static long longValue(JsonObject object, String name) {
        return object.get(name).getAsLong();
    }

    private static Optional<URI> optionalUri(JsonObject object, String name) {
        String value = optionalString(object, name);
        return value == null ? Optional.empty() : Optional.of(URI.create(value));
    }

    private static java.util.Set<String> stringSet(JsonObject object, String name) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        array(object, name).forEach(value -> values.add(value.getAsString()));
        return values;
    }
}
