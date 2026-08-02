package dev.loaderbridge.repository.modrinth;

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

class ModrinthRepositoryProviderTest {
    private static final String SHA1 = "0123456789abcdef0123456789abcdef01234567";
    private static final String SHA512 = "0123456789abcdef".repeat(8);

    @TempDir
    Path cache;

    @Test
    void searchesFabric1211ModsByDownloadRank() throws Exception {
        FakeTransport transport = new FakeTransport();
        transport.searchJson = """
                {"hits":[{"project_id":"AABBCCDD","slug":"example-mod","title":"Example Mod",
                "project_type":"mod","downloads":42}],"offset":0,"limit":1,"total_hits":1}
                """;
        ModrinthRepositoryProvider provider = new ModrinthRepositoryProvider(transport);

        var page = provider.search(new RepositoryQuery("1.21.1", "fabric", 0, 1,
                RepositorySort.DOWNLOADS));

        assertThat(page.projects()).singleElement().satisfies(project -> {
            assertThat(project.projectId()).isEqualTo("AABBCCDD");
            assertThat(project.downloads()).isEqualTo(42);
        });
        String query = URLDecoder.decode(transport.requested.getFirst().getRawQuery(), StandardCharsets.UTF_8);
        assertThat(query).contains("index=downloads", "limit=1", "versions:1.21.1",
                "categories:fabric", "project_type:mod");
    }

    @Test
    void mapsVersionFilesHashesAndDependenciesFromOfficialShape() throws Exception {
        FakeTransport transport = new FakeTransport();
        transport.versionsJson = """
                [{"version_number":"1.2.3","version_type":"release","id":"VERSION1",
                "project_id":"PROJECT1","date_published":"2026-08-01T00:00:00Z",
                "game_versions":["1.21.1"],"loaders":["fabric"],
                "dependencies":[{"project_id":"DEPENDENCY","version_id":null,"dependency_type":"required"}],
                "files":[
                  {"hashes":{"sha1":"%s"},"url":"https://cdn.modrinth.com/source.jar",
                   "filename":"source.jar","primary":false,"size":10,"file_type":"sources-jar"},
                  {"hashes":{"sha1":"%s","sha512":"%s"},
                   "url":"https://cdn.modrinth.com/data/PROJECT1/versions/VERSION1/example.jar",
                   "filename":"example.jar","primary":true,"size":7,"file_type":null}
                ]}]
                """.formatted(SHA1, SHA1, SHA512);
        ModrinthRepositoryProvider provider = new ModrinthRepositoryProvider(transport);

        var versions = provider.versions("PROJECT1", "1.21.1", "fabric");

        assertThat(versions).singleElement().satisfies(artifact -> {
            assertThat(artifact.fileName()).isEqualTo("example.jar");
            assertThat(artifact.preferredHash()).hasValueSatisfying(hash ->
                    assertThat(hash.value()).isEqualTo(SHA512));
            assertThat(artifact.dependencies()).singleElement().satisfies(dependency -> {
                assertThat(dependency.projectId()).isEqualTo("DEPENDENCY");
                assertThat(dependency.kind()).isEqualTo(DependencyKind.REQUIRED);
            });
        });
    }

    @Test
    void resolvesPinnedVersionIdsForDependencyTraversal() throws Exception {
        FakeTransport transport = new FakeTransport();
        String versionArray = versionJson(7, SHA1);
        transport.versionsJson = versionArray.substring(versionArray.indexOf('{'),
                versionArray.lastIndexOf('}') + 1);
        ModrinthRepositoryProvider provider = new ModrinthRepositoryProvider(transport);

        var artifact = provider.versionById("VERSION1");

        assertThat(artifact).hasValueSatisfying(value -> assertThat(value.versionId()).isEqualTo("VERSION1"));
        assertThat(transport.requested.getFirst().getPath()).endsWith("/version/VERSION1");
    }

    @Test
    void downloadsToHashAddressedCacheAndVerifiesContent() throws Exception {
        byte[] bytes = "fixture".getBytes(StandardCharsets.UTF_8);
        FakeTransport transport = new FakeTransport();
        transport.downloadBytes = bytes;
        transport.versionsJson = versionJson(bytes.length, sha1(bytes));
        ModrinthRepositoryProvider provider = new ModrinthRepositoryProvider(transport);
        var artifact = provider.versions("PROJECT1", "1.21.1", "fabric").getFirst();

        Path downloaded = provider.download(artifact, cache);
        Path cached = provider.download(artifact, cache);

        assertThat(downloaded).isEqualTo(cached);
        assertThat(Files.readAllBytes(downloaded)).isEqualTo(bytes);
        assertThat(transport.downloadCount).isEqualTo(1);
    }

    @Test
    void rejectsMalformedRepositoryResponsesAsIoFailures() {
        FakeTransport transport = new FakeTransport();
        transport.searchJson = "{\"hits\":{},\"offset\":0,\"total_hits\":1}";
        ModrinthRepositoryProvider provider = new ModrinthRepositoryProvider(transport);

        assertThatThrownBy(() -> provider.search(new RepositoryQuery("1.21.1", "fabric", 0, 1,
                RepositorySort.DOWNLOADS))).isInstanceOf(IOException.class)
                .hasMessageContaining("Modrinth");
    }

    @Test
    void refusesDownloadsFromHostsOutsideModrinthCdn() throws Exception {
        byte[] bytes = "fixture".getBytes(StandardCharsets.UTF_8);
        FakeTransport transport = new FakeTransport();
        transport.versionsJson = versionJson(bytes.length, sha1(bytes));
        ModrinthRepositoryProvider provider = new ModrinthRepositoryProvider(transport);
        RepositoryArtifact trusted = provider.versions("PROJECT1", "1.21.1", "fabric").getFirst();
        RepositoryArtifact untrusted = new RepositoryArtifact(trusted.repository(), trusted.projectId(),
                trusted.versionId(), trusted.versionNumber(), trusted.fileName(),
                URI.create("https://example.invalid/example.jar"), trusted.size(), trusted.hashes(),
                trusted.publishedAt(), trusted.releaseChannel(), trusted.gameVersions(), trusted.loaders(),
                trusted.dependencies());

        assertThatThrownBy(() -> provider.download(untrusted, cache)).isInstanceOf(IOException.class)
                .hasMessageContaining("Untrusted");
        assertThat(transport.downloadCount).isZero();
    }

    private static String versionJson(long size, String sha1) {
        return """
                [{"version_number":"1.0.0","version_type":"release","id":"VERSION1",
                "project_id":"PROJECT1","date_published":"2026-08-01T00:00:00Z",
                "game_versions":["1.21.1"],"loaders":["fabric"],"dependencies":[],
                "files":[{"hashes":{"sha1":"%s"},
                "url":"https://cdn.modrinth.com/data/PROJECT1/versions/VERSION1/example.jar",
                "filename":"example.jar","primary":true,"size":%d,"file_type":null}]}]
                """.formatted(sha1, size);
    }

    private static String sha1(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
    }

    private static final class FakeTransport implements ModrinthTransport {
        private final List<URI> requested = new ArrayList<>();
        private String searchJson;
        private String versionsJson;
        private byte[] downloadBytes = new byte[0];
        private int downloadCount;

        @Override
        public byte[] read(URI uri, long maximumBytes) {
            requested.add(uri);
            String value = uri.getPath().endsWith("/search") ? searchJson : versionsJson;
            return value.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public void download(URI uri, Path destination, long maximumBytes) throws IOException {
            requested.add(uri);
            downloadCount++;
            Files.write(destination, downloadBytes);
        }
    }
}
