package dev.loaderbridge.api.repository;

import java.util.Objects;

public record RepositoryId(String value) implements Comparable<RepositoryId> {
    public RepositoryId {
        Objects.requireNonNull(value, "value");
        if (!value.matches("[a-z0-9][a-z0-9._-]{0,63}")) {
            throw new IllegalArgumentException("Invalid repository ID: " + value);
        }
    }

    @Override
    public int compareTo(RepositoryId other) {
        return value.compareTo(other.value);
    }
}
