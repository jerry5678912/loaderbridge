package dev.loaderbridge.fabric.metadata;

import dev.loaderbridge.api.BridgePhase;
import dev.loaderbridge.api.Diagnostic;
import dev.loaderbridge.api.DiagnosticSeverity;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Resolves already-present Fabric mods. It deliberately does not download dependencies. */
public final class FabricDependencyResolver {
    public List<Diagnostic> resolve(
            Path artifact,
            List<FabricModMetadata> mods,
            Map<String, String> builtinVersions) {
        return resolve(artifact, mods, builtinVersions, builtinVersions.keySet());
    }

    public List<Diagnostic> resolve(
            Path artifact,
            List<FabricModMetadata> mods,
            Map<String, String> builtinVersions,
            Set<String> exclusiveBuiltinIds) {
        Map<String, InstalledVersion> installed = installedVersions(mods, builtinVersions);
        List<Diagnostic> diagnostics = new ArrayList<>();
        addIdentityCollisions(artifact, mods, builtinVersions, exclusiveBuiltinIds, diagnostics);

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

    private static void addIdentityCollisions(
            Path artifact,
            List<FabricModMetadata> mods,
            Map<String, String> builtinVersions,
            Set<String> exclusiveBuiltinIds,
            List<Diagnostic> diagnostics) {
        Map<String, IdentityClaim> claims = new LinkedHashMap<>();
        exclusiveBuiltinIds.forEach(id -> {
            String version = builtinVersions.get(id);
            if (version == null) {
                throw new IllegalArgumentException(
                        "Exclusive built-in identity has no installed version: " + id);
            }
            claims.put(id, new IdentityClaim(id, version, ClaimKind.BUILTIN));
        });
        for (FabricModMetadata mod : mods) {
            claimIdentity(artifact, diagnostics, claims, mod, mod.id(), ClaimKind.PRIMARY);
            java.util.Set<String> localAliases = new java.util.HashSet<>();
            for (String alias : mod.provides()) {
                if (!localAliases.add(alias)) {
                    diagnostics.add(identityCollision(artifact, mod, alias,
                            "is declared more than once by " + mod.id()));
                    continue;
                }
                claimIdentity(artifact, diagnostics, claims, mod, alias, ClaimKind.ALIAS);
            }
        }
    }

    private static void claimIdentity(
            Path artifact,
            List<Diagnostic> diagnostics,
            Map<String, IdentityClaim> claims,
            FabricModMetadata mod,
            String identity,
            ClaimKind kind) {
        IdentityClaim claim = new IdentityClaim(mod.id(), mod.version(), kind);
        IdentityClaim prior = claims.putIfAbsent(identity, claim);
        if (prior == null) return;

        boolean duplicateCandidate = prior.kind() != ClaimKind.BUILTIN
                && prior.kind() == kind
                && prior.modId().equals(mod.id())
                && prior.version().equals(mod.version());
        if (!duplicateCandidate) {
            diagnostics.add(identityCollision(artifact, mod, identity,
                    "is claimed by both " + prior.modId() + " and " + mod.id()));
        }
    }

    private static Diagnostic identityCollision(
            Path artifact, FabricModMetadata mod, String identity, String detail) {
        return diagnostic(DiagnosticSeverity.ERROR, "LB-DEPS-006", mod, artifact,
                "Fabric mod identity " + identity + " " + detail);
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

    private record IdentityClaim(String modId, String version, ClaimKind kind) {}

    private enum ClaimKind {
        BUILTIN,
        PRIMARY,
        ALIAS
    }
}
