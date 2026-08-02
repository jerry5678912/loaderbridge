package dev.loaderbridge.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ForgeServerInstanceInstallerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void installsPinnedForgeIntoAnEmptyDisposableDirectory() throws Exception {
        Path installer = fakeInstaller("installer.jar");
        Path instance = temporaryDirectory.resolve("instance");

        ForgeInstallationResult result = new ForgeServerInstanceInstaller().install(installer,
                sha256(installer), instance, Duration.ofSeconds(10));

        assertThat(result.succeeded()).isTrue();
        assertThat(result.code()).isEqualTo("LB-LAB-INSTALL-PASS");
        assertThat(instance.resolve(windows() ? "run.bat" : "run.sh")).isRegularFile();
        assertThat(result.transcript()).content(StandardCharsets.UTF_8).contains("FAKE_FORGE_INSTALLED");
    }

    @Test
    void refusesAnInstallerWhoseChecksumDoesNotMatch() throws Exception {
        Path installer = fakeInstaller("untrusted.jar");
        Path instance = temporaryDirectory.resolve("instance");

        ForgeInstallationResult result = new ForgeServerInstanceInstaller().install(installer,
                "0".repeat(64), instance, Duration.ofSeconds(10));

        assertThat(result.succeeded()).isFalse();
        assertThat(result.code()).isEqualTo("LB-LAB-INSTALL-002");
        assertThat(instance).doesNotExist();
    }

    @Test
    void refusesToInstallOverAUsedInstance() throws Exception {
        Path installer = fakeInstaller("installer.jar");
        Path instance = Files.createDirectories(temporaryDirectory.resolve("instance"));
        Files.writeString(instance.resolve("world.txt"), "user data", StandardCharsets.UTF_8);

        ForgeInstallationResult result = new ForgeServerInstanceInstaller().install(installer,
                sha256(installer), instance, Duration.ofSeconds(10));

        assertThat(result.succeeded()).isFalse();
        assertThat(result.code()).isEqualTo("LB-LAB-INSTALL-003");
        assertThat(instance.resolve("world.txt")).content().isEqualTo("user data");
    }

    private Path fakeInstaller(String name) throws Exception {
        Path jar = temporaryDirectory.resolve(name);
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, FakeInstaller.class.getName());
        String classResource = FakeInstaller.class.getName().replace('.', '/') + ".class";
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar), manifest);
                InputStream input = FakeInstaller.class.getClassLoader().getResourceAsStream(classResource)) {
            output.putNextEntry(new JarEntry(classResource));
            output.write(input.readAllBytes());
            output.closeEntry();
        }
        return jar;
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static boolean windows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
    }

    public static final class FakeInstaller {
        private FakeInstaller() {
        }

        public static void main(String[] args) throws Exception {
            Path instance = Path.of(args[args.length - 1]);
            Files.createDirectories(instance);
            boolean onWindows = System.getProperty("os.name", "")
                    .toLowerCase(java.util.Locale.ROOT).contains("win");
            Path script = instance.resolve(onWindows ? "run.bat" : "run.sh");
            Files.writeString(script, onWindows ? "@echo off\r\n" : "#!/usr/bin/env sh\n",
                    StandardCharsets.UTF_8);
            System.out.println("FAKE_FORGE_INSTALLED");
        }
    }
}
