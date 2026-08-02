package dev.loaderbridge.api.repository;

import java.util.List;
import java.util.Objects;

public record RepositoryPage(List<RepositoryProject> projects, int offset, int total) {
    public RepositoryPage {
        projects = List.copyOf(Objects.requireNonNull(projects, "projects"));
        if (offset < 0 || total < 0 || offset > total || projects.size() > 100
                || (long) offset + projects.size() > total) {
            throw new IllegalArgumentException("Invalid repository page bounds");
        }
    }
}
