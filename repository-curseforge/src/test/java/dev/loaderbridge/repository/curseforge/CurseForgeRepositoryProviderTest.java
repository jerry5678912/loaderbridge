package dev.loaderbridge.repository.curseforge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.loaderbridge.api.repository.DependencyKind;
import dev.loaderbridge.api.repository.RepositoryArtifact;
import dev.loaderbridge.api.repository.RepositoryQuery;
import dev.loaderbridge.api.repository.RepositorySort;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CurseForgeRepositoryProviderTest {
    private static final String SHA1 = "0123456789abcdef0123456789abcdef01234567";

    @TempDir
    Path cache;

    @Test
    void searchesMinecraftFabricModsByDownloadRank() throws Exception {
        FakeTransport transport = new FakeTransport();
        transport.searchJson = """
                {"data":[{"id":238222,"name":"Example Mod","slug":"example-mod",
                "downloadCount":42,"classId":6,"isAvailable":true,
                "links":{"sourceUrl":"https://github.com/example/mod"}}],
                "pagination":{"index":0,"pageSize":1,"resultCount":1,"totalCount":1}}
                """;
        var provider = new CurseForgeRepositoryProvider(transport);

        var page = provider.search(new RepositoryQuery("1.21.1", "fabric", 0, 1,
                RepositorySort.DOWNLOADS));

        assertThat(page.projects()).singleElement().satisfies(project -> {
            assertThat(project.projectId()).isEqualTo("238222");
            assertThat(project.sourceUrl()).contains(URI.create("https://github.com/example/mod"));
        });
        String query = URLDecoder.decode(transport.requested.getFirst().getRawQuery(), StandardCharsets.UTF_8);
        assertThat(query).contains("gameId=432", "classId=6", "gameVersion=1.21.1",
                "modLoaderType=4", "sortField=6", "sortOrder=desc");
    }

    @Test
    void reportsMissingApiKeyWithoutMakingARequest() {
        HttpCurseForgeTransport transport = new HttpCurseForgeTransport(name -> null,
                "CURSEFORGE_API_KEY");

        assertThatThrownBy(() -> transport.read(URI.create("https://api.curseforge.com/v1/games"), 100))
                .isInstanceOf(IOException.class).hasMessageContaining("CURSEFORGE_API_KEY");
    }

    @Test
    void mapsEligibleFilesHashesAndAllDependencyRelations() throws Exception {
        FakeTransport transport = new FakeTransport();
        transport.filesJson = filesJson(7, SHA1, "https://edge.forgecdn.net/files/1/2/example.jar",
                """
                [{"modId":10,"relationType":3},{"modId":11,"relationType":2},
                 {"modId":12,"relationType":5},{"modId":13,"relationType":1}]
                """);
        var provider = new CurseForgeRepositoryProvider(transport);

        var versions = provider.versions("238222", "1.21.1", "fabric");

        assertThat(versions).singleElement().satisfies(artifact -> {
            assertThat(artifact.versionId()).isEqualTo("12345");
            assertThat(artifact.preferredHash()).hasValueSatisfying(hash ->
                    assertThat(hash.value()).isEqualTo(SHA1));
            assertThat(artifact.dependencies()).extracting(dependency -> dependency.kind())
                    .containsExactly(DependencyKind.REQUIRED, DependencyKind.OPTIONAL,
                            DependencyKind.INCOMPATIBLE, DependencyKind.EMBEDDED);
            assertThat(artifact.isEligibleFabric1211()).isTrue();
        });
    }

    @Test
    void resolvesDownloadUrlWhenFileMetadataOmitsIt() throws Exception {
        FakeTransport transport = new FakeTransport();
        transport.filesJson = filesJson(7, SHA1, null, "[]");
        transport.downloadUrlJson = "{\"data\":\"https://edge.forgecdn.net/files/1/2/example.jar\"}";

        var artifact = new CurseForgeRepositoryProvider(transport)
                .versions("238222", "1.21.1", "fabric").getFirst();

        assertThat(artifact.downloadUrl().getHost()).isEqualTo("edge.forgecdn.net");
        assertThat(transport.requested).anyMatch(uri -> uri.getPath().endsWith("/download-url"));
    }

    @Test
    void downloadsToVerifiedHashAddressedCache() throws Exception {
        byte[] bytes = "fixture".getBytes(StandardCharsets.UTF_8);
        FakeTransport transport = new FakeTransport();
        transport.downloadBytes = bytes;
        transport.filesJson = filesJson(bytes.length, sha1(bytes),
                "https://edge.forgecdn.net/files/1/2/example.jar", "[]");
        var provider = new CurseForgeRepositoryProvider(transport);
        var artifact = provider.versions("238222", "1.21.1", "fabric").getFirst();

        Path downloaded = provider.download(artifact, cache);
        Path cached = provider.download(artifact, cache);

        assertThat(downloaded).isEqualTo(cached);
        assertThat(Files.readAllBytes(downloaded)).isEqualTo(bytes);
        assertThat(transport.downloadCount).isEqualTo(1);
    }

    @Test
    void refusesDownloadsOutsideCurseForgeCdn() throws Exception {
        FakeTransport transport = new FakeTransport();
        transport.filesJson = filesJson(7, SHA1,
                "https://edge.forgecdn.net/files/1/2/example.jar", "[]");
        var provider = new CurseForgeRepositoryProvider(transport);
        RepositoryArtifact trusted = provider.versions("238222", "1.21.1", "fabric").getFirst();
        RepositoryArtifact untrusted = new RepositoryArtifact(trusted.repository(), trusted.projectId(),
                trusted.versionId(), trusted.versionNumber(), trusted.fileName(),
                URI.create("https://forgecdn.net.attacker.invalid/example.jar"), trusted.size(),
                trusted.hashes(), trusted.publishedAt(), trusted.releaseChannel(), trusted.gameVersions(),
                trusted.loaders(), trusted.dependencies());

        assertThatThrownBy(() -> provider.download(untrusted, cache)).isInstanceOf(IOException.class)
                .hasMessageContaining("Untrusted");
        assertThat(transport.downloadCount).isZero();
    }

    @Test
    void skipsAlphaSourceAndUnhashedFiles() throws Exception {
        FakeTransport transport = new FakeTransport();
        transport.filesJson = """
                {"data":[
                  %s,
                  %s,
                  %s
                ],"pagination":{"index":0,"pageSize":3,"resultCount":3,"totalCount":3}}
                """.formatted(fileObject(1, "alpha.jar", 3, true, true),
                        fileObject(2, "mod-sources.jar", 1, true, true),
                        fileObject(3, "mod.jar", 1, true, false));

        assertThat(new CurseForgeRepositoryProvider(transport)
                .versions("238222", "1.21.1", "fabric")).isEmpty();
    }

    private static String filesJson(long size, String sha1, String url, String dependencies) {
        String downloadUrl = url == null ? "null" : "\"" + url + "\"";
        return """
                {"data":[{"id":12345,"isAvailable":true,"displayName":"1.2.3",
                "fileName":"example.jar","releaseType":1,"hashes":[{"value":"%s","algo":1}],
                "fileDate":"2026-08-01T00:00:00Z","fileLength":%d,"downloadUrl":%s,
                "gameVersions":["1.21.1","Fabric"],"dependencies":%s}],
                "pagination":{"index":0,"pageSize":1,"resultCount":1,"totalCount":1}}
                """.formatted(sha1, size, downloadUrl, dependencies);
    }

    private static String fileObject(int id, String name, int releaseType, boolean hash,
            boolean fabric) {
        return """
                {"id":%d,"isAvailable":true,"displayName":"1.0","fileName":"%s",
                "releaseType":%d,"hashes":%s,"fileDate":"2026-08-01T00:00:00Z",
                "fileLength":7,"downloadUrl":"https://edge.forgecdn.net/file.jar",
                "gameVersions":["1.21.1"%s],"dependencies":[]}
                """.formatted(id, name, releaseType,
                        hash ? "[{\"value\":\"" + SHA1 + "\",\"algo\":1}]" : "[]",
                        fabric ? ",\"Fabric\"" : "");
    }

    private static String sha1(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
    }

    private static final class FakeTransport implements CurseForgeTransport {
        private final List<URI> requested = new ArrayList<>();
        private String searchJson;
        private String filesJson;
        private String downloadUrlJson;
        private byte[] downloadBytes = new byte[0];
        private int downloadCount;

        @Override
        public byte[] read(URI uri, long maximumBytes) {
            requested.add(uri);
            String response = uri.getPath().endsWith("/search") ? searchJson
                    : uri.getPath().endsWith("/download-url") ? downloadUrlJson : filesJson;
            return response.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public void download(URI uri, Path destination, long maximumBytes) throws IOException {
            requested.add(uri);
            downloadCount++;
            Files.write(destination, downloadBytes);
        }
    }
}
