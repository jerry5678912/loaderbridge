package dev.loaderbridge.fabric.remap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MixinExtrasRuntimeResolverTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void verifiesAndReusesPinnedArtifact() throws Exception {
        byte[] official = "controlled-mixinextras".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        AtomicInteger requests = new AtomicInteger();
        MixinExtrasRuntimeResolver resolver = new MixinExtrasRuntimeResolver((uri, maximumBytes) -> {
            requests.incrementAndGet();
            return official;
        }, "test", URI.create("https://example.invalid/mixinextras.jar"), sha256(official));

        ResolvedRuntimeLibrary first = resolver.resolve(temporaryDirectory, true);
        ResolvedRuntimeLibrary second = resolver.resolve(temporaryDirectory, false);

        assertThat(first).isEqualTo(second);
        assertThat(first.version()).isEqualTo("test");
        assertThat(first.sha256()).isEqualTo(sha256(official));
        assertThat(requests).hasValue(1);

        Files.write(first.path(), new byte[] {9});
        assertThatThrownBy(() -> resolver.resolve(temporaryDirectory, false))
                .isInstanceOf(ArtifactVerificationException.class)
                .hasMessageContaining("SHA-256 mismatch");
    }

    @Test
    void rejectsContentOutsideThePinnedChecksum() {
        MixinExtrasRuntimeResolver resolver = new MixinExtrasRuntimeResolver(
                (uri, maximumBytes) -> new byte[] {1, 2, 3});

        assertThatThrownBy(() -> resolver.resolve(temporaryDirectory, true))
                .isInstanceOf(ArtifactVerificationException.class)
                .hasMessageContaining("SHA-256 mismatch");
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
