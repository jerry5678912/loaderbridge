package dev.loaderbridge.catalog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.loaderbridge.api.repository.RepositoryArtifact;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class RepositoryResolutionLockCodec {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public byte[] encode(RepositoryArtifact root, ResolvedDependencyGraph graph) {
        JsonObject document = new JsonObject();
        document.addProperty("schemaVersion", 1);
        document.addProperty("minecraftVersion", "1.21.1");
        document.addProperty("loader", "fabric");
        document.addProperty("root", root.repository().value() + ":" + root.versionId());
        JsonArray artifacts = new JsonArray();
        graph.installationOrder().forEach(artifact -> artifacts.add(artifact(artifact)));
        document.add("artifacts", artifacts);
        document.add("resolvedEdges", resolvedEdges(graph));
        return (JSON.toJson(document) + "\n").getBytes(StandardCharsets.UTF_8);
    }

    public void write(RepositoryArtifact root, ResolvedDependencyGraph graph, Path destination)
            throws IOException {
        Path absolute = destination.toAbsolutePath().normalize();
        Path parent = absolute.getParent();
        if (parent == null) {
            throw new IOException("Resolution lock needs a parent directory");
        }
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, absolute.getFileName().toString(), ".tmp");
        try {
            Files.write(temporary, encode(root, graph));
            try {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    static JsonObject artifact(RepositoryArtifact artifact) {
        JsonObject value = new JsonObject();
        value.addProperty("repository", artifact.repository().value());
        value.addProperty("projectId", artifact.projectId());
        value.addProperty("versionId", artifact.versionId());
        value.addProperty("versionNumber", artifact.versionNumber());
        value.addProperty("fileName", artifact.fileName());
        value.addProperty("url", artifact.downloadUrl().toString());
        value.addProperty("size", artifact.size());
        JsonObject hashes = new JsonObject();
        artifact.hashes().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
                .forEach(hash -> hashes.addProperty(hash.getKey().name().toLowerCase(
                        java.util.Locale.ROOT), hash.getValue()));
        value.add("hashes", hashes);
        value.addProperty("publishedAt", artifact.publishedAt().toString());
        value.addProperty("releaseChannel", artifact.releaseChannel().name().toLowerCase(
                java.util.Locale.ROOT));
        value.add("gameVersions", strings(artifact.gameVersions()));
        value.add("loaders", strings(artifact.loaders()));
        JsonArray dependencies = new JsonArray();
        artifact.dependencies().stream().sorted(java.util.Comparator
                .comparing((dev.loaderbridge.api.repository.RepositoryDependency dependency) ->
                        dependency.projectId() == null ? "" : dependency.projectId())
                .thenComparing(dependency -> dependency.versionId() == null ? "" : dependency.versionId())
                .thenComparing(dependency -> dependency.kind().name()))
                .forEach(dependency -> {
                    JsonObject item = new JsonObject();
                    if (dependency.projectId() != null) {
                        item.addProperty("projectId", dependency.projectId());
                    }
                    if (dependency.versionId() != null) {
                        item.addProperty("versionId", dependency.versionId());
                    }
                    item.addProperty("kind", dependency.kind().name().toLowerCase(java.util.Locale.ROOT));
                    dependencies.add(item);
                });
        value.add("dependencies", dependencies);
        return value;
    }

    static JsonArray resolvedEdges(ResolvedDependencyGraph graph) {
        JsonArray edges = new JsonArray();
        graph.resolvedEdges().stream().sorted(java.util.Comparator
                .comparing((ResolvedDependencyEdge edge) -> edge.ownerRepository().value())
                .thenComparing(ResolvedDependencyEdge::ownerVersionId)
                .thenComparing(edge -> edge.declaredDependency().projectId() == null
                        ? "" : edge.declaredDependency().projectId())
                .thenComparing(edge -> edge.declaredDependency().versionId() == null
                        ? "" : edge.declaredDependency().versionId())
                .thenComparing(ResolvedDependencyEdge::resolvedVersionId)).forEach(edge -> {
                    JsonObject value = new JsonObject();
                    value.addProperty("owner", edge.ownerRepository().value() + ":"
                            + edge.ownerVersionId());
                    JsonObject declared = new JsonObject();
                    if (edge.declaredDependency().projectId() != null) {
                        declared.addProperty("projectId", edge.declaredDependency().projectId());
                    }
                    if (edge.declaredDependency().versionId() != null) {
                        declared.addProperty("versionId", edge.declaredDependency().versionId());
                    }
                    declared.addProperty("kind", edge.declaredDependency().kind().name()
                            .toLowerCase(java.util.Locale.ROOT));
                    value.add("declared", declared);
                    value.addProperty("resolved", edge.resolvedRepository().value() + ":"
                            + edge.resolvedVersionId());
                    edges.add(value);
                });
        return edges;
    }

    private static JsonArray strings(java.util.Set<String> source) {
        JsonArray values = new JsonArray();
        source.stream().sorted().forEach(values::add);
        return values;
    }
}
