package dev.loaderbridge.api;

import java.util.List;

public record BridgePlan(
        AdapterDescriptor adapter,
        List<ModInspection> mods,
        List<BridgeCapability> requiredCapabilities,
        List<Diagnostic> diagnostics) {
    public BridgePlan {
        mods = List.copyOf(mods);
        requiredCapabilities = List.copyOf(requiredCapabilities);
        diagnostics = List.copyOf(diagnostics);
    }

    public boolean canPrepare() {
        return diagnostics.stream().noneMatch(d -> d.severity() == DiagnosticSeverity.ERROR);
    }
}
