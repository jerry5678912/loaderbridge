package dev.loaderbridge.fabric.metadata;

public record JarReadLimits(int maxEntries, long maxEntryBytes, long maxTotalBytes, int maxNestedDepth) {
    // Content mods legitimately contain tens of thousands of tiny model, blockstate, recipe,
    // loot-table, and texture entries. Keep the independent per-entry and expanded-size caps.
    public static final JarReadLimits DEFAULT =
            new JarReadLimits(65_536, 64L << 20, 512L << 20, 8);

    public JarReadLimits {
        if (maxEntries < 1 || maxEntryBytes < 1 || maxTotalBytes < maxEntryBytes || maxNestedDepth < 0) {
            throw new IllegalArgumentException("Invalid JAR read limits");
        }
    }
}
