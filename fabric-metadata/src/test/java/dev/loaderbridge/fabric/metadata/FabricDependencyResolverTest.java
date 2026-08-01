package dev.loaderbridge.fabric.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import dev.loaderbridge.api.DiagnosticSeverity;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class FabricDependencyResolverTest {
    @Test
    void acceptsInstalledDependenciesAndProvidedAliasesUsingFabricRanges() {
        FabricModMetadata library = metadata("library", "2.4.0", Map.of(), List.of("library_alias"), Map.of(), Map.of());
        FabricModMetadata consumer = metadata(
                "consumer",
                "1.0.0",
                Map.of("library_alias", List.of(">=2.0.0"), "minecraft", List.of("~1.21.1")),
                List.of(),
                Map.of(),
                Map.of());

        List<dev.loaderbridge.api.Diagnostic> diagnostics = new FabricDependencyResolver().resolve(
                Path.of("mods"),
                List.of(library, consumer),
                Map.of("minecraft", "1.21.1", "fabricloader", "0.16.14", "java", "21"));

        assertThat(diagnostics).isEmpty();
    }

    @Test
    void reportsMissingAndMismatchedRequiredDependenciesAsStableErrors() {
        FabricModMetadata consumer = metadata(
                "consumer",
                "1.0.0",
                Map.of("missing", List.of("*"), "fabricloader", List.of(">=0.17.0")),
                List.of(),
                Map.of(),
                Map.of());

        List<dev.loaderbridge.api.Diagnostic> diagnostics = new FabricDependencyResolver().resolve(
                Path.of("consumer.jar"),
                List.of(consumer),
                Map.of("minecraft", "1.21.1", "fabricloader", "0.16.14", "java", "21"));

        assertThat(diagnostics).extracting(dev.loaderbridge.api.Diagnostic::code)
                .containsExactly("LB-DEPS-001", "LB-DEPS-002");
        assertThat(diagnostics).allMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR);
    }

    @Test
    void distinguishesHardBreaksFromAdvisoryConflicts() {
        FabricModMetadata target = metadata("target", "1.0.0", Map.of(), List.of(), Map.of(), Map.of());
        FabricModMetadata consumer = metadata(
                "consumer",
                "1.0.0",
                Map.of(),
                List.of(),
                Map.of("target", List.of("*")),
                Map.of("target", List.of("*")));

        List<dev.loaderbridge.api.Diagnostic> diagnostics = new FabricDependencyResolver().resolve(
                Path.of("mods"), List.of(target, consumer), Map.of());

        assertThat(diagnostics).extracting(dev.loaderbridge.api.Diagnostic::code)
                .containsExactly("LB-DEPS-003", "LB-DEPS-004");
        assertThat(diagnostics).extracting(dev.loaderbridge.api.Diagnostic::severity)
                .containsExactly(DiagnosticSeverity.ERROR, DiagnosticSeverity.WARNING);
    }

    private static FabricModMetadata metadata(
            String id,
            String version,
            Map<String, List<String>> depends,
            List<String> provides,
            Map<String, List<String>> breaks,
            Map<String, List<String>> conflicts) {
        return new FabricModMetadata(
                1,
                id,
                version,
                id,
                "*",
                Map.of(),
                new FabricDependencies(depends, Map.of(), Map.of(), breaks, conflicts),
                provides,
                List.of(),
                Optional.empty(),
                List.of(),
                Map.of());
    }
}
