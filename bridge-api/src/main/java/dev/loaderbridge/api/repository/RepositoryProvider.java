package dev.loaderbridge.api.repository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Launcher-neutral source for catalog projects and immutable mod artifacts.
 *
 * <p>Provider implementations map their official repository contracts into this boundary:
 * https://docs.modrinth.com/api/operations/searchprojects/ and
 * https://docs.curseforge.com/rest-api/.
 */
public interface RepositoryProvider {
    RepositoryId id();

    RepositoryPage search(RepositoryQuery query) throws IOException;

    List<RepositoryArtifact> versions(String projectId, String minecraftVersion, String loader)
            throws IOException;

    /** Resolve a repository-native immutable version/file ID when dependency metadata pins one. */
    default Optional<RepositoryArtifact> versionById(String versionId) throws IOException {
        return Optional.empty();
    }

    Path download(RepositoryArtifact artifact, Path cacheDirectory) throws IOException;
}
