package dev.loaderbridge.api;

import java.nio.file.Path;
import java.util.List;

public record PreparationResult(List<Path> artifacts, Path report, List<Diagnostic> diagnostics) {
    public PreparationResult {
        artifacts = List.copyOf(artifacts);
        diagnostics = List.copyOf(diagnostics);
    }

    public boolean succeeded() {
        return diagnostics.stream().noneMatch(d -> d.severity() == DiagnosticSeverity.ERROR);
    }
}
