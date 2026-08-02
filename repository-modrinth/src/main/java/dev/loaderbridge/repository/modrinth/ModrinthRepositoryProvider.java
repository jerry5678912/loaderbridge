package dev.loaderbridge.repository.modrinth;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.loaderbridge.api.repository.ArtifactHash;
import dev.loaderbridge.api.repository.DependencyKind;
import dev.loaderbridge.api.repository.HashAlgorithm;
import dev.loaderbridge.api.repository.ReleaseChannel;
import dev.loaderbridge.api.repository.RepositoryArtifact;
import dev.loaderbridge.api.repository.RepositoryDependency;
import dev.loaderbridge.api.repository.RepositoryId;
import dev.loaderbridge.api.repository.RepositoryPage;
import dev.loaderbridge.api.repository.RepositoryProject;
import dev.loaderbridge.api.repository.RepositoryProvider;
import dev.loaderbridge.api.repository.RepositoryQuery;
import dev.loaderbridge.api.repository.RepositorySort;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Modrinth v2 provider following https://docs.modrinth.com/api/. */
public final class ModrinthRepositoryProvider implements RepositoryProvider {
    private static final RepositoryId ID = new RepositoryId("modrinth");
    private static final URI API = URI.create("https://api.modrinth.com/v2/");
    private static final long MAXIMUM_METADATA_BYTES = 8L << 20;
    private final ModrinthTransport transport;

    public ModrinthRepositoryProvider() {
        this(new HttpModrinthTransport());
    }

    ModrinthRepositoryProvider(ModrinthTransport transport) {
        this.transport = java.util.Objects.requireNonNull(transport, "transport");
    }

    @Override
    public RepositoryId id() {
        return ID;
    }

    @Override
    public RepositoryPage search(RepositoryQuery query) throws IOException {
        String facets = "[[\"categories:" + jsonFacet(query.loader()) + "\"],"
                + "[\"versions:" + jsonFacet(query.minecraftVersion()) + "\"],[\"project_type:mod\"]]";
        String index = query.sort() == RepositorySort.DOWNLOADS ? "downloads" : "updated";
        URI uri = endpoint("search?facets=" + parameter(facets) + "&index=" + index
                + "&offset=" + query.offset() + "&limit=" + query.limit());
        try {
            JsonObject response = object(read(uri));
            JsonArray hits = array(response, "hits");
            List<RepositoryProject> projects = new ArrayList<>();
            for (JsonElement value : hits) {
                JsonObject hit = value.getAsJsonObject();
                if (!"mod".equals(string(hit, "project_type"))) {
                    continue;
                }
                projects.add(new RepositoryProject(ID, string(hit, "project_id"), string(hit, "slug"),
                        string(hit, "title"), number(hit, "downloads"), Optional.empty()));
            }
            return new RepositoryPage(projects, integer(response, "offset"), integer(response, "total_hits"));
        } catch (RuntimeException exception) {
            throw malformed("search response", exception);
        }
    }

    @Override
    public List<RepositoryArtifact> versions(String projectId, String minecraftVersion, String loader)
            throws IOException {
        String pathId = pathSegment(projectId);
        String gameVersions = "[\"" + jsonFacet(minecraftVersion) + "\"]";
        String loaders = "[\"" + jsonFacet(loader) + "\"]";
        URI uri = endpoint("project/" + pathId + "/version?game_versions=" + parameter(gameVersions)
                + "&loaders=" + parameter(loaders) + "&include_changelog=false");
        try {
            JsonArray response = array(read(uri));
            List<RepositoryArtifact> artifacts = new ArrayList<>();
            for (JsonElement value : response) {
                parseVersion(value.getAsJsonObject()).ifPresent(artifacts::add);
            }
            return artifacts.stream().sorted(Comparator.comparing(RepositoryArtifact::publishedAt).reversed())
                    .toList();
        } catch (RuntimeException exception) {
            throw malformed("version response", exception);
        }
    }

    @Override
    public Path download(RepositoryArtifact artifact, Path cacheDirectory) throws IOException {
        if (!artifact.repository().equals(ID)) {
            throw new IOException("Modrinth provider cannot download " + artifact.repository().value()
                    + " artifacts");
        }
        if (!"cdn.modrinth.com".equalsIgnoreCase(artifact.downloadUrl().getHost())) {
            throw new IOException("Untrusted Modrinth download host");
        }
        ArtifactHash hash = artifact.preferredHash().orElseThrow(() ->
                new IOException("Modrinth artifact has no supported hash"));
        Path root = cacheDirectory.toAbsolutePath().normalize().resolve("modrinth")
                .resolve(hash.algorithm().name().toLowerCase(Locale.ROOT)).resolve(hash.value());
        Path destination = root.resolve(artifact.fileName()).normalize();
        if (!destination.startsWith(root)) {
            throw new IOException("Unsafe Modrinth cache path");
        }
        if (Files.isRegularFile(destination) && verifies(destination, artifact, hash)) {
            return destination;
        }
        Files.createDirectories(root);
        Path temporary = Files.createTempFile(root, artifact.fileName(), ".tmp");
        try {
            transport.download(artifact.downloadUrl(), temporary, artifact.size());
            if (!verifies(temporary, artifact, hash)) {
                throw new IOException("Downloaded Modrinth artifact failed size or hash verification");
            }
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            return destination;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while downloading Modrinth artifact", exception);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private Optional<RepositoryArtifact> parseVersion(JsonObject version) {
        Optional<JsonObject> file = selectedFile(array(version, "files"));
        if (file.isEmpty()) {
            return Optional.empty();
        }
        JsonObject selected = file.orElseThrow();
        EnumMap<HashAlgorithm, String> hashes = new EnumMap<>(HashAlgorithm.class);
        JsonObject sourceHashes = object(selected.get("hashes"));
        if (sourceHashes.has("sha1")) {
            hashes.put(HashAlgorithm.SHA1, string(sourceHashes, "sha1"));
        }
        if (sourceHashes.has("sha512")) {
            hashes.put(HashAlgorithm.SHA512, string(sourceHashes, "sha512"));
        }
        List<RepositoryDependency> dependencies = new ArrayList<>();
        for (JsonElement value : array(version, "dependencies")) {
            JsonObject dependency = value.getAsJsonObject();
            String project = nullableString(dependency, "project_id");
            String pinnedVersion = nullableString(dependency, "version_id");
            if (project != null || pinnedVersion != null) {
                dependencies.add(new RepositoryDependency(project, pinnedVersion,
                        dependencyKind(string(dependency, "dependency_type"))));
            }
        }
        return Optional.of(new RepositoryArtifact(ID, string(version, "project_id"), string(version, "id"),
                string(version, "version_number"), string(selected, "filename"),
                URI.create(string(selected, "url")), number(selected, "size"), hashes,
                Instant.parse(string(version, "date_published")),
                ReleaseChannel.valueOf(string(version, "version_type").toUpperCase(Locale.ROOT)),
                strings(array(version, "game_versions")), strings(array(version, "loaders")), dependencies));
    }

    private static Optional<JsonObject> selectedFile(JsonArray files) {
        List<JsonObject> candidates = new ArrayList<>();
        for (JsonElement value : files) {
            JsonObject file = value.getAsJsonObject();
            String type = nullableString(file, "file_type");
            if (string(file, "filename").toLowerCase(Locale.ROOT).endsWith(".jar")
                    && (type == null || type.equals("unknown"))) {
                candidates.add(file);
            }
        }
        return candidates.stream().filter(file -> booleanValue(file, "primary")).findFirst()
                .or(() -> candidates.stream().findFirst());
    }

    private byte[] read(URI uri) throws IOException {
        try {
            return transport.read(uri, MAXIMUM_METADATA_BYTES);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while reading Modrinth metadata", exception);
        }
    }

    private static boolean verifies(Path path, RepositoryArtifact artifact, ArtifactHash expected)
            throws IOException {
        if (Files.size(path) != artifact.size()) {
            return false;
        }
        try (var input = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance(expected.algorithm() == HashAlgorithm.SHA512
                    ? "SHA-512" : "SHA-1");
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                digest.update(buffer, 0, count);
            }
            return HexFormat.of().formatHex(digest.digest()).equals(expected.value());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Required hash algorithm is unavailable", exception);
        }
    }

    private static DependencyKind dependencyKind(String value) {
        return switch (value) {
            case "required" -> DependencyKind.REQUIRED;
            case "optional" -> DependencyKind.OPTIONAL;
            case "incompatible" -> DependencyKind.INCOMPATIBLE;
            case "embedded" -> DependencyKind.EMBEDDED;
            default -> throw new IllegalArgumentException("Unknown Modrinth dependency type: " + value);
        };
    }

    private static URI endpoint(String relative) {
        return API.resolve(relative);
    }

    private static String parameter(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String pathSegment(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]{1,256}")) {
            throw new IllegalArgumentException("Invalid Modrinth project ID");
        }
        return value;
    }

    private static String jsonFacet(String value) {
        if (value == null || !value.matches("[A-Za-z0-9._+ -]{1,64}")) {
            throw new IllegalArgumentException("Invalid Modrinth facet value");
        }
        return value;
    }

    private static JsonElement parsed(byte[] bytes) {
        return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
    }

    private static JsonObject object(byte[] bytes) {
        return parsed(bytes).getAsJsonObject();
    }

    private static JsonObject object(JsonElement value) {
        return value.getAsJsonObject();
    }

    private static JsonArray array(byte[] bytes) {
        return parsed(bytes).getAsJsonArray();
    }

    private static JsonArray array(JsonObject object, String name) {
        return object.getAsJsonArray(name);
    }

    private static String string(JsonObject object, String name) {
        return object.get(name).getAsString();
    }

    private static String nullableString(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    private static long number(JsonObject object, String name) {
        return object.get(name).getAsLong();
    }

    private static int integer(JsonObject object, String name) {
        return object.get(name).getAsInt();
    }

    private static boolean booleanValue(JsonObject object, String name) {
        return object.get(name).getAsBoolean();
    }

    private static Set<String> strings(JsonArray values) {
        java.util.LinkedHashSet<String> strings = new java.util.LinkedHashSet<>();
        values.forEach(value -> strings.add(value.getAsString()));
        return Set.copyOf(strings);
    }

    private static IOException malformed(String label, RuntimeException cause) {
        return new IOException("Malformed Modrinth " + label + ": " + cause.getMessage(), cause);
    }
}
