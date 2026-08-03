package dev.loaderbridge.api.repository;

import java.net.URI;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record RepositoryArtifact(RepositoryId repository, String projectId, String versionId,
        String versionNumber, String fileName, URI downloadUrl, long size,
        Map<HashAlgorithm, String> hashes, Instant publishedAt, ReleaseChannel releaseChannel,
        Set<String> gameVersions, Set<String> loaders, List<RepositoryDependency> dependencies) {
    private static final long MAX_ARTIFACT_SIZE = 1L << 30;

    public RepositoryArtifact {
        Objects.requireNonNull(repository, "repository");
        projectId = identifier(projectId, "projectId");
        versionId = identifier(versionId, "versionId");
        versionNumber = text(versionNumber, "versionNumber", 256);
        fileName = fileName(fileName);
        downloadUrl = secureUrl(downloadUrl);
        if (size <= 0 || size > MAX_ARTIFACT_SIZE) {
            throw new IllegalArgumentException("Artifact size must be between 1 byte and 1 GiB");
        }
        hashes = validatedHashes(hashes);
        Objects.requireNonNull(publishedAt, "publishedAt");
        Objects.requireNonNull(releaseChannel, "releaseChannel");
        gameVersions = Set.copyOf(Objects.requireNonNull(gameVersions, "gameVersions"));
        loaders = Set.copyOf(Objects.requireNonNull(loaders, "loaders"));
        dependencies = List.copyOf(Objects.requireNonNull(dependencies, "dependencies"));
    }

    public boolean isEligibleFabric1211() {
        return isEligibleFor("1.21.1", "fabric");
    }

    public boolean isEligibleFor(String minecraftVersion, String loaderId) {
        Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        Objects.requireNonNull(loaderId, "loaderId");
        return releaseChannel != ReleaseChannel.ALPHA && gameVersions.contains(minecraftVersion)
                && loaders.stream().anyMatch(loader -> loader.equalsIgnoreCase(loaderId));
    }

    public Optional<ArtifactHash> preferredHash() {
        if (hashes.containsKey(HashAlgorithm.SHA512)) {
            return Optional.of(new ArtifactHash(HashAlgorithm.SHA512, hashes.get(HashAlgorithm.SHA512)));
        }
        if (hashes.containsKey(HashAlgorithm.SHA1)) {
            return Optional.of(new ArtifactHash(HashAlgorithm.SHA1, hashes.get(HashAlgorithm.SHA1)));
        }
        return Optional.empty();
    }

    private static Map<HashAlgorithm, String> validatedHashes(Map<HashAlgorithm, String> source) {
        Objects.requireNonNull(source, "hashes");
        EnumMap<HashAlgorithm, String> validated = new EnumMap<>(HashAlgorithm.class);
        source.forEach((algorithm, value) -> {
            ArtifactHash hash = new ArtifactHash(algorithm, value);
            validated.put(hash.algorithm(), hash.value());
        });
        if (validated.isEmpty()) {
            throw new IllegalArgumentException("At least one verified hash is required");
        }
        return Map.copyOf(validated);
    }

    private static String identifier(String value, String name) {
        String validated = text(value, name, 256);
        if (validated.contains("/") || validated.contains("\\")) {
            throw new IllegalArgumentException("Invalid " + name);
        }
        return validated;
    }

    private static String fileName(String value) {
        String validated = text(value, "fileName", 512);
        if (validated.contains("/") || validated.contains("\\") || validated.equals(".")
                || validated.equals("..")) {
            throw new IllegalArgumentException("Invalid artifact filename");
        }
        return validated;
    }

    private static String text(String value, String name, int maximumLength) {
        Objects.requireNonNull(value, name);
        String validated = value.strip();
        if (validated.isEmpty() || validated.length() > maximumLength) {
            throw new IllegalArgumentException("Invalid " + name);
        }
        return validated;
    }

    private static URI secureUrl(URI value) {
        Objects.requireNonNull(value, "downloadUrl");
        if (!value.isAbsolute() || !"https".equalsIgnoreCase(value.getScheme()) || value.getHost() == null
                || value.getUserInfo() != null || value.getFragment() != null) {
            throw new IllegalArgumentException("Artifact download URL must be an absolute HTTPS URL");
        }
        return value;
    }
}
