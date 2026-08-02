package dev.loaderbridge.fabric.remap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MinecraftArtifactResolverTest {
    private static final URI MANIFEST = URI.create(
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json");

    @TempDir
    Path temporaryDirectory;

    @Test
    void resolvesVerifiesAndReusesPinnedMinecraftArtifacts() throws Exception {
        URI versionUrl = URI.create("https://piston-meta.mojang.com/v1/packages/version.json");
        URI clientUrl = URI.create("https://piston-data.mojang.com/v1/objects/client.jar");
        URI mappingsUrl = URI.create("https://piston-data.mojang.com/v1/objects/client.txt");
        byte[] client = "client-jar".getBytes(StandardCharsets.UTF_8);
        byte[] mappings = "named.Class -> a:".getBytes(StandardCharsets.UTF_8);
        byte[] version = ("{\"downloads\":{"
                + "\"client\":{\"sha1\":\"" + sha1(client) + "\",\"size\":" + client.length
                + ",\"url\":\"" + clientUrl + "\"},"
                + "\"client_mappings\":{\"sha1\":\"" + sha1(mappings) + "\",\"size\":"
                + mappings.length + ",\"url\":\"" + mappingsUrl + "\"}}}")
                .getBytes(StandardCharsets.UTF_8);
        byte[] manifest = ("{\"versions\":[{\"id\":\"1.21.1\",\"type\":\"release\","
                + "\"url\":\"" + versionUrl + "\",\"sha1\":\"" + sha1(version) + "\"}]}")
                .getBytes(StandardCharsets.UTF_8);
        Map<URI, byte[]> responses = new LinkedHashMap<>();
        responses.put(MANIFEST, manifest);
        responses.put(versionUrl, version);
        responses.put(clientUrl, client);
        responses.put(mappingsUrl, mappings);
        AtomicInteger requests = new AtomicInteger();
        ArtifactTransport transport = (uri, maximumBytes) -> {
            requests.incrementAndGet();
            byte[] response = responses.get(uri);
            if (response == null) {
                throw new IllegalArgumentException("Unexpected URI " + uri);
            }
            return response;
        };
        MinecraftArtifactResolver resolver = new MinecraftArtifactResolver(transport);

        ResolvedMinecraftArtifacts first = resolver.resolve("1.21.1", temporaryDirectory, true);
        int firstRequestCount = requests.get();
        ResolvedMinecraftArtifacts second = resolver.resolve("1.21.1", temporaryDirectory, false);

        assertThat(Files.readAllBytes(first.clientJar().path())).isEqualTo(client);
        assertThat(Files.readAllBytes(first.clientMappings().path())).isEqualTo(mappings);
        assertThat(first.version()).isEqualTo("1.21.1");
        assertThat(second).isEqualTo(first);
        assertThat(requests).hasValue(firstRequestCount);
        assertThat(firstRequestCount).isEqualTo(4);
    }

    @Test
    void rejectsArtifactWhoseContentDoesNotMatchDeclaredChecksum() {
        URI versionUrl = URI.create("https://piston-meta.mojang.com/version.json");
        URI clientUrl = URI.create("https://piston-data.mojang.com/client.jar");
        byte[] version = ("{\"downloads\":{\"client\":{\"sha1\":\"0000000000000000000000000000000000000000\","
                + "\"size\":3,\"url\":\"" + clientUrl + "\"},"
                + "\"client_mappings\":{\"sha1\":\"0000000000000000000000000000000000000000\","
                + "\"size\":3,\"url\":\"" + clientUrl + "\"}}}").getBytes(StandardCharsets.UTF_8);
        byte[] manifest = ("{\"versions\":[{\"id\":\"1.21.1\",\"url\":\"" + versionUrl
                + "\",\"sha1\":\"" + sha1(version) + "\"}]}").getBytes(StandardCharsets.UTF_8);
        Map<URI, byte[]> responses = Map.of(MANIFEST, manifest, versionUrl, version,
                clientUrl, "bad".getBytes(StandardCharsets.UTF_8));
        MinecraftArtifactResolver resolver = new MinecraftArtifactResolver(
                (uri, maximumBytes) -> responses.get(uri));

        assertThatThrownBy(() -> resolver.resolve("1.21.1", temporaryDirectory, true))
                .isInstanceOf(ArtifactVerificationException.class)
                .hasMessageContaining("SHA-1");
    }

    private static String sha1(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }
}
