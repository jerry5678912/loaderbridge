package dev.loaderbridge.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ForgeServerVerifierTest {
    @TempDir
    Path instance;

    @Test
    void observesReadyThenStopsAndRequiresWorldSave() throws Exception {
        writeLaunchScript(true);
        List<String> output = new ArrayList<>();

        VerificationResult result = new ForgeServerVerifier().verify(instance, Duration.ofSeconds(5), output::add,
                List.of("LOADERBRIDGE_FIXTURE_MAIN_READY"));

        assertThat(result.succeeded()).isTrue();
        assertThat(output).anyMatch(line -> line.contains("LOADERBRIDGE_FIXTURE_MAIN_READY"));
    }

    @Test
    void reportsProcessThatExitsBeforeReady() throws Exception {
        writeLaunchScript(false);

        VerificationResult result = new ForgeServerVerifier().verify(instance, Duration.ofSeconds(5), ignored -> { });

        assertThat(result.succeeded()).isFalse();
        assertThat(result.diagnosticCode()).isEqualTo("LB-VERIFY-005");
    }

    @Test
    void rejectsCleanForgeRunMissingRequiredBridgeMarker() throws Exception {
        writeLaunchScript(true);

        VerificationResult result = new ForgeServerVerifier().verify(instance, Duration.ofSeconds(5), ignored -> { },
                List.of("LOADERBRIDGE_FIXTURE_SERVER_READY"));

        assertThat(result.succeeded()).isFalse();
        assertThat(result.diagnosticCode()).isEqualTo("LB-VERIFY-008");
    }

    private void writeLaunchScript(boolean ready) throws Exception {
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        Path script = instance.resolve(windows ? "run.bat" : "run.sh");
        String contents;
        if (windows) {
            contents = ready
                    ? "@echo off\r\necho LOADERBRIDGE_FIXTURE_MAIN_READY\r\necho Done (0.1s)! For help, type help\r\nset /p command=\r\necho ThreadedAnvilChunkStorage: All dimensions are saved\r\n"
                    : "@echo off\r\necho Forge failed during loading\r\nexit /b 1\r\n";
        } else {
            contents = ready
                    ? "#!/usr/bin/env sh\nprintf '%s\\n' LOADERBRIDGE_FIXTURE_MAIN_READY\nprintf '%s\\n' 'Done (0.1s)! For help, type help'\nread command\nprintf '%s\\n' 'ThreadedAnvilChunkStorage: All dimensions are saved'\n"
                    : "#!/usr/bin/env sh\nprintf '%s\\n' 'Forge failed during loading'\nexit 1\n";
        }
        Files.writeString(script, contents, StandardCharsets.UTF_8);
    }
}
