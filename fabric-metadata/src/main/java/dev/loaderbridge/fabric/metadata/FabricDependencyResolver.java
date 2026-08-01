package dev.loaderbridge.fabric.metadata;

import dev.loaderbridge.api.BridgePhase;
import dev.loaderbridge.api.Diagnostic;
import dev.loaderbridge.api.DiagnosticSeverity;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Resolves already-present Fabric mods. It deliberately does not download dependencies. */
public final class FabricDependencyResolver {
    public List<Diagnostic> resolve(
            Path artifact,
            List<FabricModMetadata> mods,
            Map<String, String> builtinVersions) {
        Map<String, InstalledVersion> installed = installedVersions(mods, builtinVersions);
        List<Diagnostic> diagnostics = new ArrayList<>();

        for (FabricModMetadata mod : mods) {
            addMissingDependencies(artifact, installed, diagnostics, mod);
            addMismatchedDependencies(artifact, installed, diagnostics, mod);
            addMatchingRelations(artifact, installed, diagnostics, mod, mod.dependencies().breaks(),
                    DiagnosticSeverity.ERROR, "LB-DEPS-003", "breaks");
            addMatchingRelations(artifact, installed, diagnostics, mod, mod.dependencies().conflicts(),
                    DiagnosticSeverity.WARNING, "LB-DEPS-004", "conflicts with");
            addMissingRecommendations(artifact, installed, diagnostics, mod);
        }
        return List.copyOf(diagnostics);
    }

    private static Map<String, InstalledVersion> installedVersions(
            List<FabricModMetadata> mods, Map<String, String> builtins) {
        Map<String, InstalledVersion> installed = new LinkedHashMap<>();
        builtins.forEach((id, version) -> installed.put(id, new InstalledVersion(id, version)));
        for (FabricModMetadata mod : mods) {
            InstalledVersion value = new InstalledVersion(mod.id(), mod.version());
            installed.put(mod.id(), value);
            mod.provides().forEach(alias -> installed.put(alias, value));
        }
        return installed;
    }

    private static void addMissingDependencies(
            Path artifact,
            Map<String, InstalledVersion> installed,
            List<Diagnostic> diagnostics,
            FabricModMetadata mod) {
        mod.dependencies().depends().forEach((id, ranges) -> {
            if (!installed.containsKey(id)) {
                diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "LB-DEPS-001", mod, artifact,
                        "Missing required dependency " + id + " " + String.join(" || ", ranges)));
            }
        });
    }

    private static void addMismatchedDependencies(
            Path artifact,
            Map<String, InstalledVersion> installed,
            List<Diagnostic> diagnostics,
            FabricModMetadata mod) {
        mod.dependencies().depends().forEach((id, ranges) -> {
            InstalledVersion value = installed.get(id);
            if (value != null && !FabricVersionPredicate.anyMatches(ranges, value.version())) {
                diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "LB-DEPS-002", mod, artifact,
                        "Dependency " + id + " has " + value.version() + ", requires " + String.join(" || ", ranges)));
            }
        });
    }

    private static void addMatchingRelations(
            Path artifact,
            Map<String, InstalledVersion> installed,
            List<Diagnostic> diagnostics,
            FabricModMetadata mod,
            Map<String, List<String>> relations,
            DiagnosticSeverity severity,
            String code,
            String verb) {
        relations.forEach((id, ranges) -> {
            InstalledVersion value = installed.get(id);
            if (value != null && FabricVersionPredicate.anyMatches(ranges, value.version())) {
                diagnostics.add(diagnostic(severity, code, mod, artifact,
                        mod.id() + " " + verb + " " + value.modId() + " " + value.version()));
            }
        });
    }

    private static void addMissingRecommendations(
            Path artifact,
            Map<String, InstalledVersion> installed,
            List<Diagnostic> diagnostics,
            FabricModMetadata mod) {
        mod.dependencies().recommends().forEach((id, ranges) -> {
            InstalledVersion value = installed.get(id);
            if (value == null || !FabricVersionPredicate.anyMatches(ranges, value.version())) {
                diagnostics.add(diagnostic(DiagnosticSeverity.WARNING, "LB-DEPS-005", mod, artifact,
                        "Recommended dependency is unavailable: " + id + " " + String.join(" || ", ranges)));
            }
        });
    }

    private static Diagnostic diagnostic(
            DiagnosticSeverity severity,
            String code,
            FabricModMetadata mod,
            Path artifact,
            String message) {
        return new Diagnostic(severity, code, BridgePhase.PLAN, mod.id(), artifact, message, null);
    }

    private record InstalledVersion(String modId, String version) {}
}
