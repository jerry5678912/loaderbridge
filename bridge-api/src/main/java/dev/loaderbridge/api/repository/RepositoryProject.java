package dev.loaderbridge.api.repository;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

public record RepositoryProject(RepositoryId repository, String projectId, String slug, String title,
        long downloads, Optional<URI> sourceUrl) {
    public RepositoryProject {
        Objects.requireNonNull(repository, "repository");
        projectId = required(projectId, "projectId", 256);
        slug = required(slug, "slug", 256);
        title = required(title, "title", 512);
        if (downloads < 0) {
            throw new IllegalArgumentException("Downloads cannot be negative");
        }
        sourceUrl = Objects.requireNonNull(sourceUrl, "sourceUrl").map(RepositoryProject::httpsUrl);
    }

    private static String required(String value, String name, int maximumLength) {
        Objects.requireNonNull(value, name);
        String normalized = value.strip();
        if (normalized.isEmpty() || normalized.length() > maximumLength) {
            throw new IllegalArgumentException("Invalid " + name);
        }
        return normalized;
    }

    private static URI httpsUrl(URI value) {
        if (!value.isAbsolute() || !"https".equalsIgnoreCase(value.getScheme()) || value.getHost() == null
                || value.getUserInfo() != null || value.getFragment() != null) {
            throw new IllegalArgumentException("Source URL must be absolute HTTPS");
        }
        return value;
    }
}
