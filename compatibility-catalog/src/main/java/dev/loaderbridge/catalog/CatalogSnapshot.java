package dev.loaderbridge.catalog;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record CatalogSnapshot(int schemaVersion, String snapshotId, Instant frozenAt,
        String minecraftVersion, String loader, List<CatalogEntry> entries) {
    public CatalogSnapshot {
        if (schemaVersion != 1) {
            throw new IllegalArgumentException("Unsupported catalog schema version");
        }
        snapshotId = required(snapshotId, "snapshotId");
        Objects.requireNonNull(frozenAt, "frozenAt");
        minecraftVersion = required(minecraftVersion, "minecraftVersion");
        loader = required(loader, "loader");
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
        HashSet<String> projects = new HashSet<>();
        for (int index = 0; index < entries.size(); index++) {
            CatalogEntry entry = entries.get(index);
            if (entry.rank() != index + 1) {
                throw new IllegalArgumentException("Catalog ranks must be contiguous");
            }
            String key = entry.project().repository().value() + ":" + entry.project().projectId();
            if (!projects.add(key)) {
                throw new IllegalArgumentException("Catalog contains a duplicate project");
            }
        }
    }

    private static String required(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = value.strip();
        if (normalized.isEmpty() || normalized.length() > 256) {
            throw new IllegalArgumentException("Invalid " + name);
        }
        return normalized;
    }
}
