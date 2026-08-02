package dev.loaderbridge.fabric.remap;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/** Resolves the pinned official Forge game-library distribution of MixinExtras. */
final class MixinExtrasRuntimeResolver implements RuntimeLibraryProvider {
    static final String VERSION = "0.5.4";
    static final URI URL = URI.create("https://repo1.maven.org/maven2/io/github/llamalad7/"
            + "mixinextras-forge/" + VERSION + "/mixinextras-forge-" + VERSION + ".jar");
    static final String SHA256 = "7922899a121a27f63a69a9ffe57470d8719cc52d239dfa9408e15d32a7b4c264";
    private static final long MAXIMUM_BYTES = 8L << 20;
    private final ArtifactTransport transport;
    private final String version;
    private final URI url;
    private final String expectedSha256;

    MixinExtrasRuntimeResolver() {
        this(new HttpsTransport(), VERSION, URL, SHA256);
    }

    MixinExtrasRuntimeResolver(ArtifactTransport transport) {
        this(transport, VERSION, URL, SHA256);
    }

    MixinExtrasRuntimeResolver(ArtifactTransport transport, String version, URI url,
            String expectedSha256) {
        this.transport = java.util.Objects.requireNonNull(transport, "transport");
        this.version = java.util.Objects.requireNonNull(version, "version");
        this.url = java.util.Objects.requireNonNull(url, "url");
        this.expectedSha256 = java.util.Objects.requireNonNull(expectedSha256, "expectedSha256");
        if (!version.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("Invalid runtime-library version");
        }
        if (!"https".equalsIgnoreCase(url.getScheme()) || url.getHost() == null) {
            throw new IllegalArgumentException("Runtime-library URL must be absolute HTTPS");
        }
        if (!expectedSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Runtime-library SHA-256 must be lowercase hexadecimal");
        }
    }

    @Override
    public ResolvedRuntimeLibrary resolve(Path cacheDirectory, boolean refresh) throws IOException {
        Path artifact = cacheDirectory.resolve("runtime-libraries")
                .resolve("mixinextras-forge-" + version + "-" + expectedSha256 + ".jar");
        if (!refresh && Files.isRegularFile(artifact)) {
            verifyCached(artifact);
            return resolved(artifact);
        }
        byte[] bytes;
        try {
            bytes = transport.read(url, MAXIMUM_BYTES);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while resolving MixinExtras " + version, exception);
        }
        verify(bytes);
        Files.createDirectories(artifact.getParent());
        Path temporary = Files.createTempFile(artifact.getParent(), "mixinextras-", ".tmp");
        try {
            Files.write(temporary, bytes);
            Files.move(temporary, artifact, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temporary);
        }
        return resolved(artifact);
    }

    private void verifyCached(Path artifact) throws IOException {
        long size = Files.size(artifact);
        if (size < 1 || size > MAXIMUM_BYTES) {
            throw new ArtifactVerificationException("Cached MixinExtras artifact size is invalid: " + size);
        }
        verify(Files.readAllBytes(artifact));
    }

    private ResolvedRuntimeLibrary resolved(Path artifact) {
        return new ResolvedRuntimeLibrary("mixinextras-forge", version, url, expectedSha256,
                artifact.toAbsolutePath().normalize());
    }

    private void verify(byte[] bytes) throws ArtifactVerificationException {
        String actual = sha256(bytes);
        if (!actual.equals(expectedSha256)) {
            throw new ArtifactVerificationException("MixinExtras " + version
                    + " SHA-256 mismatch: expected " + expectedSha256 + ", received " + actual);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by Java", exception);
        }
    }

    private static final class HttpsTransport implements ArtifactTransport {
        private final HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        @Override
        public byte[] read(URI uri, long maximumBytes) throws IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(60)).GET().build();
            HttpResponse<InputStream> response = client.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200 || !response.uri().equals(uri)) {
                throw new IOException("Could not download HTTPS runtime library: HTTP "
                        + response.statusCode());
            }
            try (InputStream input = response.body()) {
                byte[] bytes = input.readNBytes(Math.toIntExact(maximumBytes + 1));
                if (bytes.length > maximumBytes) {
                    throw new IOException("Runtime library exceeds download limit");
                }
                return bytes;
            }
        }
    }
}
