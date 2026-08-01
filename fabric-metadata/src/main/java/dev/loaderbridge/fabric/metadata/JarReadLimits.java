package dev.loaderbridge.fabric.metadata;

public record JarReadLimits(int maxEntries, long maxEntryBytes, long maxTotalBytes, int maxNestedDepth) {
    public static final JarReadLimits DEFAULT = new JarReadLimits(10_000, 64L << 20, 512L << 20, 8);

    public JarReadLimits {
        if (maxEntries < 1 || maxEntryBytes < 1 || maxTotalBytes < maxEntryBytes || maxNestedDepth < 0) {
            throw new IllegalArgumentException("Invalid JAR read limits");
        }
    }
}
