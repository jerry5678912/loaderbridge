package dev.loaderbridge.api;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Launcher-neutral contract implemented by a directional loader bridge. */
public interface BridgeAdapter {
    AdapterDescriptor descriptor();

    ModInspection inspect(Path artifact) throws IOException;

    BridgePlan plan(BridgeRequest request) throws IOException;

    PreparationResult prepare(BridgeRequest request, BridgePlan plan) throws IOException;

    default List<Diagnostic> diagnose(BridgeRequest request) throws IOException {
        return plan(request).diagnostics();
    }
}
