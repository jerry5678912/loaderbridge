package dev.loaderbridge.repository.curseforge;

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
import java.util.Optional;
import java.util.Set;

/** CurseForge provider following https://docs.curseforge.com/rest-api/. */
public final class CurseForgeRepositoryProvider implements RepositoryProvider {
    private static final RepositoryId ID = new RepositoryId("curseforge");
    private static final URI API = URI.create("https://api.curseforge.com/v1/");
    private static final long MAXIMUM_METADATA_BYTES = 8L << 20;
    private static final int MINECRAFT_GAME_ID = 432;
    private static final int MINECRAFT_MOD_CLASS_ID = 6;
    private static final int FORGE_LOADER = 1;
    private static final int FABRIC_LOADER = 4;
    private final CurseForgeTransport transport;

    public CurseForgeRepositoryProvider() {
        this(new HttpCurseForgeTransport());
    }

    CurseForgeRepositoryProvider(CurseForgeTransport transport) {
        this.transport = java.util.Objects.requireNonNull(transport, "transport");
    }

    @Override
    public RepositoryId id() {
        return ID;
    }

    @Override
    public RepositoryPage search(RepositoryQuery query) throws IOException {
        int loaderType = loaderType(query.loader());
        int pageSize = Math.min(query.limit(), 50);
        int sortField = query.sort() == RepositorySort.DOWNLOADS ? 6 : 3;
        URI uri = endpoint("mods/search?gameId=" + MINECRAFT_GAME_ID + "&classId="
                + MINECRAFT_MOD_CLASS_ID + "&gameVersion=" + parameter(query.minecraftVersion())
                + "&modLoaderType=" + loaderType + "&sortField=" + sortField
                + "&sortOrder=desc&index=" + query.offset() + "&pageSize=" + pageSize);
        try {
            JsonObject response = object(read(uri));
            List<RepositoryProject> projects = new ArrayList<>();
            for (JsonElement value : array(response, "data")) {
                JsonObject mod = value.getAsJsonObject();
                if (!booleanValue(mod, "isAvailable") || nullableInteger(mod, "classId") != MINECRAFT_MOD_CLASS_ID) {
                    continue;
                }
                Optional<URI> source = sourceUrl(mod);
                projects.add(new RepositoryProject(ID, Integer.toString(integer(mod, "id")),
                        string(mod, "slug"), string(mod, "name"), number(mod, "downloadCount"), source));
            }
            JsonObject pagination = object(response.get("pagination"));
            return new RepositoryPage(projects, integer(pagination, "index"),
                    integer(pagination, "totalCount"));
        } catch (RuntimeException exception) {
            throw malformed("search response", exception);
        }
    }

    @Override
    public List<RepositoryArtifact> versions(String projectId, String minecraftVersion, String loader)
            throws IOException {
        int loaderType = loaderType(loader);
        int modId = numericId(projectId);
        List<RepositoryArtifact> artifacts = new ArrayList<>();
        int index = 0;
        while (index < 10_000) {
            URI uri = endpoint("mods/" + modId + "/files?gameVersion=" + parameter(minecraftVersion)
                    + "&modLoaderType=" + loaderType + "&index=" + index + "&pageSize=50");
            JsonObject response;
            try {
                response = object(read(uri));
                for (JsonElement value : array(response, "data")) {
                    Optional<RepositoryArtifact> artifact = parseFile(modId, value.getAsJsonObject(), loader);
                    artifact.ifPresent(artifacts::add);
                }
            } catch (RuntimeException exception) {
                throw malformed("files response", exception);
            }
            JsonObject pagination = object(response.get("pagination"));
            int resultCount = integer(pagination, "resultCount");
            int totalCount = integer(pagination, "totalCount");
            if (resultCount == 0 || index + resultCount >= totalCount) {
                break;
            }
            index += resultCount;
        }
        return artifacts.stream().sorted(Comparator.comparing(RepositoryArtifact::publishedAt).reversed())
                .toList();
    }

    @Override
    public Path download(RepositoryArtifact artifact, Path cacheDirectory) throws IOException {
        if (!artifact.repository().equals(ID)) {
            throw new IOException("CurseForge provider cannot download " + artifact.repository().value()
                    + " artifacts");
        }
        if (!trustedDownloadHost(artifact.downloadUrl().getHost())) {
            throw new IOException("Untrusted CurseForge download host");
        }
        ArtifactHash hash = artifact.preferredHash().orElseThrow(() ->
                new IOException("CurseForge artifact has no supported hash"));
        Path root = cacheDirectory.toAbsolutePath().normalize().resolve("curseforge")
                .resolve(hash.algorithm().name().toLowerCase(Locale.ROOT)).resolve(hash.value());
        Path destination = root.resolve(artifact.fileName()).normalize();
        if (!destination.startsWith(root)) {
            throw new IOException("Unsafe CurseForge cache path");
        }
        if (Files.isRegularFile(destination) && verifies(destination, artifact, hash)) {
            return destination;
        }
        Files.createDirectories(root);
        Path temporary = Files.createTempFile(root, artifact.fileName(), ".tmp");
        try {
            transport.download(artifact.downloadUrl(), temporary, artifact.size());
            if (!verifies(temporary, artifact, hash)) {
                throw new IOException("Downloaded CurseForge artifact failed size or hash verification");
            }
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            return destination;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while downloading CurseForge artifact", exception);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private Optional<RepositoryArtifact> parseFile(int modId, JsonObject file, String loader) throws IOException {
        if (!booleanValue(file, "isAvailable") || integer(file, "releaseType") == 3) {
            return Optional.empty();
        }
        String fileName = string(file, "fileName");
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        if (!lowerName.endsWith(".jar") || lowerName.contains("sources") || lowerName.contains("-dev")) {
            return Optional.empty();
        }
        String downloadUrl = nullableString(file, "downloadUrl");
        if (downloadUrl == null) {
            int fileId = integer(file, "id");
            try {
                downloadUrl = string(object(read(endpoint("mods/" + modId + "/files/" + fileId
                        + "/download-url"))), "data");
            } catch (CurseForgeHttpException exception) {
                if (exception.statusCode() == 403 || exception.statusCode() == 404) {
                    return Optional.empty();
                }
                throw exception;
            } catch (RuntimeException exception) {
                throw malformed("download URL response", exception);
            }
        }
        EnumMap<HashAlgorithm, String> hashes = new EnumMap<>(HashAlgorithm.class);
        for (JsonElement value : array(file, "hashes")) {
            JsonObject hash = value.getAsJsonObject();
            if (integer(hash, "algo") == 1) {
                hashes.put(HashAlgorithm.SHA1, string(hash, "value"));
            }
        }
        if (hashes.isEmpty()) {
            return Optional.empty();
        }
        Set<String> gameVersions = strings(array(file, "gameVersions"));
        if (gameVersions.stream().noneMatch(version -> version.equalsIgnoreCase(loader))) {
            return Optional.empty();
        }
        List<RepositoryDependency> dependencies = new ArrayList<>();
        for (JsonElement value : array(file, "dependencies")) {
            JsonObject dependency = value.getAsJsonObject();
            dependencies.add(new RepositoryDependency(Integer.toString(integer(dependency, "modId")),
                    null, dependencyKind(integer(dependency, "relationType"))));
        }
        int releaseType = integer(file, "releaseType");
        return Optional.of(new RepositoryArtifact(ID, Integer.toString(modId),
                Integer.toString(integer(file, "id")), string(file, "displayName"), fileName,
                URI.create(downloadUrl), number(file, "fileLength"), hashes,
                Instant.parse(string(file, "fileDate")), releaseType == 1 ? ReleaseChannel.RELEASE
                        : ReleaseChannel.BETA, gameVersions, Set.of(loader.toLowerCase(Locale.ROOT)), dependencies));
    }

    private byte[] read(URI uri) throws IOException {
        try {
            return transport.read(uri, MAXIMUM_METADATA_BYTES);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while reading CurseForge metadata", exception);
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

    private static Optional<URI> sourceUrl(JsonObject mod) {
        JsonElement linksValue = mod.get("links");
        if (linksValue == null || linksValue.isJsonNull()) {
            return Optional.empty();
        }
        String source = nullableString(linksValue.getAsJsonObject(), "sourceUrl");
        if (source == null) {
            return Optional.empty();
        }
        URI uri = URI.create(source);
        return "https".equalsIgnoreCase(uri.getScheme()) ? Optional.of(uri) : Optional.empty();
    }

    private static DependencyKind dependencyKind(int relationType) {
        return switch (relationType) {
            case 1, 6 -> DependencyKind.EMBEDDED;
            case 2, 4 -> DependencyKind.OPTIONAL;
            case 3 -> DependencyKind.REQUIRED;
            case 5 -> DependencyKind.INCOMPATIBLE;
            default -> throw new IllegalArgumentException("Unknown CurseForge dependency relation: "
                    + relationType);
        };
    }

    private static boolean trustedDownloadHost(String host) {
        String normalized = host == null ? "" : host.toLowerCase(Locale.ROOT);
        return normalized.endsWith(".forgecdn.net") || normalized.equals("forgecdn.net");
    }

    private static int numericId(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,9}")) {
            throw new IllegalArgumentException("Invalid CurseForge project ID");
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid CurseForge project ID", exception);
        }
    }

    private static int loaderType(String loader) {
        return switch (loader.toLowerCase(Locale.ROOT)) {
            case "forge" -> FORGE_LOADER;
            case "fabric" -> FABRIC_LOADER;
            default -> throw new IllegalArgumentException("Unsupported CurseForge loader: " + loader);
        };
    }

    private static URI endpoint(String relative) {
        return API.resolve(relative);
    }

    private static String parameter(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static JsonObject object(byte[] bytes) {
        return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static JsonObject object(JsonElement value) {
        return value.getAsJsonObject();
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

    private static int nullableInteger(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value == null || value.isJsonNull() ? -1 : value.getAsInt();
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
        return new IOException("Malformed CurseForge " + label + ": " + cause.getMessage(), cause);
    }
}
