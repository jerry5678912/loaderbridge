package dev.loaderbridge.fabric.remap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/** Resolves only Mojang-owned runtime artifacts and pins their declared SHA-1 values. */
public final class MinecraftArtifactResolver {
    public static final URI VERSION_MANIFEST = URI.create(
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json");
    private static final long MAXIMUM_METADATA_BYTES = 8L << 20;
    private static final long MAXIMUM_ARTIFACT_BYTES = 1L << 30;
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final ArtifactTransport transport;

    public MinecraftArtifactResolver() {
        this(new HttpArtifactTransport());
    }

    public MinecraftArtifactResolver(ArtifactTransport transport) {
        this.transport = java.util.Objects.requireNonNull(transport, "transport");
    }

    public ResolvedMinecraftArtifacts resolve(String version, Path cacheDirectory, boolean refresh)
            throws IOException {
        Path root = cacheDirectory.resolve("minecraft").resolve(version);
        Path lockPath = root.resolve("artifacts.lock.json");
        if (!refresh && Files.isRegularFile(lockPath)) {
            return readAndVerifyLock(lockPath);
        }
        Files.createDirectories(root.resolve("artifacts"));
        try {
            JsonObject manifest = json(readHttps(VERSION_MANIFEST, MAXIMUM_METADATA_BYTES), "version manifest");
            JsonObject versionEntry = findVersion(manifest, version);
            URI versionUrl = httpsUri(requiredString(versionEntry, "url"));
            String versionSha1 = requiredSha1(versionEntry, "sha1");
            byte[] versionBytes = readHttps(versionUrl, MAXIMUM_METADATA_BYTES);
            verify(versionBytes, versionSha1, versionBytes.length, "version metadata");
            JsonObject downloads = requiredObject(json(versionBytes, "version metadata"), "downloads");
            ResolvedArtifact client = resolveDownload("client", requiredObject(downloads, "client"), root);
            ResolvedArtifact mappings = resolveDownload("client_mappings",
                    requiredObject(downloads, "client_mappings"), root);
            ResolvedMinecraftArtifacts resolved = new ResolvedMinecraftArtifacts(version, client, mappings);
            writeAtomically(lockPath, JSON.toJson(new LockData(version, LockArtifact.from(client),
                    LockArtifact.from(mappings))).getBytes(StandardCharsets.UTF_8));
            return resolved;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while resolving Minecraft " + version, exception);
        }
    }

    private ResolvedArtifact resolveDownload(String id, JsonObject data, Path root)
            throws IOException, InterruptedException {
        String sha1 = requiredSha1(data, "sha1");
        long size = requiredSize(data);
        URI url = httpsUri(requiredString(data, "url"));
        byte[] bytes = readHttps(url, Math.min(MAXIMUM_ARTIFACT_BYTES, size + 1));
        verify(bytes, sha1, size, id);
        String suffix = id.equals("client") ? ".jar" : ".txt";
        Path path = root.resolve("artifacts").resolve(sha1 + suffix);
        writeAtomically(path, bytes);
        return new ResolvedArtifact(id, url, sha1, size, path.toAbsolutePath().normalize());
    }

    private ResolvedMinecraftArtifacts readAndVerifyLock(Path lockPath) throws IOException {
        LockData lock = JSON.fromJson(Files.readString(lockPath), LockData.class);
        ResolvedArtifact client = lock.client().resolve("client");
        ResolvedArtifact mappings = lock.clientMappings().resolve("client_mappings");
        verify(Files.readAllBytes(client.path()), client.sha1(), client.size(), client.id());
        verify(Files.readAllBytes(mappings.path()), mappings.sha1(), mappings.size(), mappings.id());
        return new ResolvedMinecraftArtifacts(lock.version(), client, mappings);
    }

    private byte[] readHttps(URI uri, long maximumBytes) throws IOException, InterruptedException {
        if (!uri.getScheme().equalsIgnoreCase("https")) {
            throw new ArtifactVerificationException("Artifact URL must use HTTPS: " + uri);
        }
        return transport.read(uri, maximumBytes);
    }

    private static JsonObject findVersion(JsonObject manifest, String version) throws IOException {
        for (JsonElement candidate : manifest.getAsJsonArray("versions")) {
            JsonObject object = candidate.getAsJsonObject();
            if (version.equals(requiredString(object, "id"))) {
                return object;
            }
        }
        throw new ArtifactVerificationException("Minecraft version is absent from Mojang manifest: " + version);
    }

    private static JsonObject json(byte[] bytes, String label) throws IOException {
        try {
            return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new ArtifactVerificationException("Malformed " + label + ": " + exception.getMessage());
        }
    }

    private static JsonObject requiredObject(JsonObject parent, String name) throws IOException {
        JsonElement value = parent.get(name);
        if (value == null || !value.isJsonObject()) {
            throw new ArtifactVerificationException("Missing object in Mojang metadata: " + name);
        }
        return value.getAsJsonObject();
    }

    private static String requiredString(JsonObject parent, String name) throws IOException {
        JsonElement value = parent.get(name);
        if (value == null || !value.isJsonPrimitive()) {
            throw new ArtifactVerificationException("Missing string in Mojang metadata: " + name);
        }
        return value.getAsString();
    }

    private static String requiredSha1(JsonObject parent, String name) throws IOException {
        String value = requiredString(parent, name).toLowerCase(java.util.Locale.ROOT);
        if (!value.matches("[0-9a-f]{40}")) {
            throw new ArtifactVerificationException("Invalid SHA-1 in Mojang metadata: " + value);
        }
        return value;
    }

    private static long requiredSize(JsonObject data) throws IOException {
        try {
            long size = data.get("size").getAsLong();
            if (size < 1 || size > MAXIMUM_ARTIFACT_BYTES) {
                throw new ArtifactVerificationException("Artifact size is outside safety limits: " + size);
            }
            return size;
        } catch (NullPointerException | NumberFormatException exception) {
            throw new ArtifactVerificationException("Missing or invalid artifact size");
        }
    }

    private static URI httpsUri(String value) throws IOException {
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw new ArtifactVerificationException("Artifact URL must be absolute HTTPS: " + value);
            }
            return uri;
        } catch (IllegalArgumentException exception) {
            throw new ArtifactVerificationException("Invalid artifact URL: " + value);
        }
    }

    private static void verify(byte[] bytes, String expectedSha1, long expectedSize, String label)
            throws IOException {
        if (bytes.length != expectedSize) {
            throw new ArtifactVerificationException(label + " size mismatch: expected " + expectedSize
                    + ", received " + bytes.length);
        }
        String actual = sha1(bytes);
        if (!actual.equals(expectedSha1)) {
            throw new ArtifactVerificationException(label + " SHA-1 mismatch: expected " + expectedSha1
                    + ", received " + actual);
        }
    }

    private static String sha1(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 is required by Java", exception);
        }
    }

    private static void writeAtomically(Path destination, byte[] bytes) throws IOException {
        Files.createDirectories(destination.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(destination.toAbsolutePath().getParent(),
                destination.getFileName().toString(), ".tmp");
        try {
            Files.write(temporary, bytes);
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private record LockData(String version, LockArtifact client, LockArtifact clientMappings) {}

    private record LockArtifact(String url, String sha1, long size, String path) {
        static LockArtifact from(ResolvedArtifact artifact) {
            return new LockArtifact(artifact.url().toString(), artifact.sha1(), artifact.size(),
                    artifact.path().toString());
        }

        ResolvedArtifact resolve(String id) throws IOException {
            return new ResolvedArtifact(id, httpsUri(url), sha1, size,
                    Path.of(path).toAbsolutePath().normalize());
        }
    }

    private static final class HttpArtifactTransport implements ArtifactTransport {
        private final HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        @Override
        public byte[] read(URI uri, long maximumBytes) throws IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(5)).GET().build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                response.body().close();
                throw new IOException("HTTP " + response.statusCode() + " while downloading " + uri);
            }
            try (InputStream input = response.body()) {
                byte[] bytes = input.readNBytes(Math.toIntExact(maximumBytes + 1));
                if (bytes.length > maximumBytes) {
                    throw new ArtifactVerificationException("Download exceeds safety limit: " + uri);
                }
                return bytes;
            }
        }
    }
}
