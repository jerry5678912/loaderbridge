package dev.loaderbridge.api.repository;

import java.util.Objects;

public record RepositoryQuery(String minecraftVersion, String loader, int offset, int limit,
        RepositorySort sort) {
    public RepositoryQuery {
        minecraftVersion = required(minecraftVersion, "minecraftVersion");
        loader = required(loader, "loader");
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Page limit must be between 1 and 100");
        }
        Objects.requireNonNull(sort, "sort");
    }

    private static String required(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = value.strip();
        if (normalized.isEmpty() || normalized.length() > 64) {
            throw new IllegalArgumentException("Invalid " + name);
        }
        return normalized;
    }
}
