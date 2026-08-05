package dev.loaderbridge.catalog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.loaderbridge.api.repository.RepositoryArtifact;
import dev.loaderbridge.api.repository.RepositoryId;
import dev.loaderbridge.api.repository.RepositoryPage;
import dev.loaderbridge.api.repository.RepositoryQuery;
import dev.loaderbridge.api.repository.RepositorySort;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Deterministic, bounded JSON storage for offline catalog reproduction inputs. */
public final class CatalogInputCaptureCodec {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final long MAXIMUM_CAPTURE_BYTES = 128L << 20;
    private static final int MAXIMUM_REQUESTS = 50_000;

    public byte[] encode(CatalogInputCapture capture) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", capture.schemaVersion());
        root.addProperty("snapshotId", capture.snapshotId());
        root.addProperty("frozenAt", capture.frozenAt().toString());
        root.addProperty("targetSize", capture.targetSize());
        root.addProperty("repositoryQuota", capture.repositoryQuota());
        JsonArray searches = new JsonArray();
        capture.searches().forEach(item -> searches.add(search(item)));
        root.add("searches", searches);
        JsonArray versions = new JsonArray();
        capture.versions().forEach(item -> versions.add(versions(item)));
        root.add("versions", versions);
        JsonArray pinned = new JsonArray();
        capture.pinnedVersions().forEach(item -> pinned.add(pinned(item)));
        root.add("pinnedVersions", pinned);
        return (JSON.toJson(root) + "\n").getBytes(StandardCharsets.UTF_8);
    }

    public void write(CatalogInputCapture capture, Path destination) throws IOException {
        Path absolute = destination.toAbsolutePath().normalize();
        Path parent = absolute.getParent();
        if (parent == null) {
            throw new IOException("Catalog input capture needs a parent directory");
        }
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, absolute.getFileName().toString(), ".tmp");
        try {
            Files.write(temporary, encode(capture));
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

    public CatalogInputCapture read(Path source) throws IOException {
        if (!Files.isRegularFile(source)) {
            throw new IOException("Catalog input capture is not a readable file: " + source);
        }
        long size = Files.size(source);
        if (size <= 0 || size > MAXIMUM_CAPTURE_BYTES) {
            throw new IOException("Catalog input capture size is outside the 128 MiB limit");
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(source,
                    StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray encodedSearches = array(root, "searches");
            JsonArray encodedVersions = array(root, "versions");
            JsonArray encodedPinned = array(root, "pinnedVersions");
            if ((long) encodedSearches.size() + encodedVersions.size() + encodedPinned.size()
                    > MAXIMUM_REQUESTS) {
                throw new IllegalArgumentException("Catalog input capture exceeds request limit");
            }
            List<CatalogInputCapture.CapturedSearch> searches = new ArrayList<>();
            for (var encoded : encodedSearches) {
                searches.add(decodeSearch(encoded.getAsJsonObject()));
            }
            List<CatalogInputCapture.CapturedVersions> versions = new ArrayList<>();
            for (var encoded : encodedVersions) {
                versions.add(decodeVersions(encoded.getAsJsonObject()));
            }
            List<CatalogInputCapture.CapturedPinnedVersion> pinned = new ArrayList<>();
            for (var encoded : encodedPinned) {
                pinned.add(decodePinned(encoded.getAsJsonObject()));
            }
            return new CatalogInputCapture(integer(root, "schemaVersion"), string(root, "snapshotId"),
                    Instant.parse(string(root, "frozenAt")), integer(root, "targetSize"),
                    integer(root, "repositoryQuota"), searches, versions, pinned);
        } catch (RuntimeException exception) {
            throw new IOException("Malformed catalog input capture: " + exception.getMessage(), exception);
        }
    }

    private static JsonObject search(CatalogInputCapture.CapturedSearch item) {
        JsonObject value = new JsonObject();
        value.addProperty("repository", item.repository().value());
        JsonObject query = new JsonObject();
        query.addProperty("minecraftVersion", item.query().minecraftVersion());
        query.addProperty("loader", item.query().loader());
        query.addProperty("offset", item.query().offset());
        query.addProperty("limit", item.query().limit());
        query.addProperty("sort", item.query().sort().name().toLowerCase(java.util.Locale.ROOT));
        value.add("query", query);
        JsonObject result = new JsonObject();
        result.addProperty("offset", item.result().offset());
        result.addProperty("total", item.result().total());
        JsonArray projects = new JsonArray();
        item.result().projects().forEach(project ->
                projects.add(CatalogSnapshotCodec.encodeProject(project)));
        result.add("projects", projects);
        value.add("result", result);
        return value;
    }

    private static CatalogInputCapture.CapturedSearch decodeSearch(JsonObject value) {
        RepositoryId repository = new RepositoryId(string(value, "repository"));
        JsonObject queryValue = object(value, "query");
        RepositoryQuery query = new RepositoryQuery(string(queryValue, "minecraftVersion"),
                string(queryValue, "loader"), integer(queryValue, "offset"),
                integer(queryValue, "limit"), RepositorySort.valueOf(
                        string(queryValue, "sort").toUpperCase(java.util.Locale.ROOT)));
        JsonObject resultValue = object(value, "result");
        List<dev.loaderbridge.api.repository.RepositoryProject> projects = new ArrayList<>();
        array(resultValue, "projects").forEach(project ->
                projects.add(CatalogSnapshotCodec.decodeProject(project.getAsJsonObject())));
        return new CatalogInputCapture.CapturedSearch(repository, query,
                new RepositoryPage(projects, integer(resultValue, "offset"),
                        integer(resultValue, "total")));
    }

    private static JsonObject versions(CatalogInputCapture.CapturedVersions item) {
        JsonObject value = request(value(item.repository(), item.projectId()), item.minecraftVersion(),
                item.loader());
        JsonArray result = new JsonArray();
        item.result().forEach(artifact -> result.add(CatalogSnapshotCodec.encodeArtifact(artifact)));
        value.add("result", result);
        return value;
    }

    private static CatalogInputCapture.CapturedVersions decodeVersions(JsonObject value) {
        RepositoryId repository = new RepositoryId(string(value, "repository"));
        String projectId = string(value, "projectId");
        List<RepositoryArtifact> artifacts = new ArrayList<>();
        array(value, "result").forEach(artifact -> artifacts.add(CatalogSnapshotCodec.decodeArtifact(
                repository, projectId, artifact.getAsJsonObject())));
        return new CatalogInputCapture.CapturedVersions(repository, projectId,
                string(value, "minecraftVersion"), string(value, "loader"), artifacts);
    }

    private static JsonObject pinned(CatalogInputCapture.CapturedPinnedVersion item) {
        JsonObject value = new JsonObject();
        value.addProperty("repository", item.repository().value());
        value.addProperty("versionId", item.versionId());
        item.result().ifPresent(artifact -> {
            value.addProperty("projectId", artifact.projectId());
            value.add("result", CatalogSnapshotCodec.encodeArtifact(artifact));
        });
        if (item.result().isEmpty()) {
            value.add("result", com.google.gson.JsonNull.INSTANCE);
        }
        return value;
    }

    private static CatalogInputCapture.CapturedPinnedVersion decodePinned(JsonObject value) {
        RepositoryId repository = new RepositoryId(string(value, "repository"));
        String versionId = string(value, "versionId");
        var encoded = value.get("result");
        Optional<RepositoryArtifact> result = encoded == null || encoded.isJsonNull()
                ? Optional.empty() : Optional.of(CatalogSnapshotCodec.decodeArtifact(repository,
                        string(value, "projectId"), encoded.getAsJsonObject()));
        return new CatalogInputCapture.CapturedPinnedVersion(repository, versionId, result);
    }

    private static JsonObject value(RepositoryId repository, String projectId) {
        JsonObject value = new JsonObject();
        value.addProperty("repository", repository.value());
        value.addProperty("projectId", projectId);
        return value;
    }

    private static JsonObject request(JsonObject value, String minecraftVersion, String loader) {
        value.addProperty("minecraftVersion", minecraftVersion);
        value.addProperty("loader", loader);
        return value;
    }

    private static JsonObject object(JsonObject value, String name) {
        return value.getAsJsonObject(name);
    }

    private static JsonArray array(JsonObject value, String name) {
        return value.getAsJsonArray(name);
    }

    private static String string(JsonObject value, String name) {
        return value.get(name).getAsString();
    }

    private static int integer(JsonObject value, String name) {
        return value.get(name).getAsInt();
    }
}
