package dev.loaderbridge.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class BridgeAdapterContractTest {
    @Test
    void modelsAnExtensibleDirectionalAdapterWithoutLoaderEnums() {
        AdapterDescriptor descriptor = new AdapterDescriptor(
                "fabric-to-forge",
                "1.0",
                new LoaderId("fabric"),
                new LoaderId("forge"),
                "[1.21.1,1.21.2)",
                "[52.1.0,53.0.0)",
                List.of(BridgeCapability.METADATA, BridgeCapability.REMAPPING));

        assertThat(descriptor.sourceLoader().value()).isEqualTo("fabric");
        assertThat(descriptor.targetLoader().value()).isEqualTo("forge");
        assertThat(descriptor.capabilities()).containsExactly(
                BridgeCapability.METADATA, BridgeCapability.REMAPPING);
    }

    @Test
    void diagnosticsCarryStableMachineReadableContext() {
        Diagnostic diagnostic = new Diagnostic(
                DiagnosticSeverity.ERROR,
                "LB-META-001",
                BridgePhase.INSPECT,
                "example",
                Path.of("example.jar"),
                "fabric.mod.json is missing",
                null);

        assertThat(diagnostic.code()).isEqualTo("LB-META-001");
        assertThat(diagnostic.phase()).isEqualTo(BridgePhase.INSPECT);
        assertThat(diagnostic.artifact()).hasFileName("example.jar");
    }
}
