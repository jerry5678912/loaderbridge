package net.fabricmc.loader.api;

import dev.loaderbridge.fabric.runtime.BridgeSemanticVersion;

public interface Version extends Comparable<Version> {
    String getFriendlyString();

    static Version parse(String string) throws VersionParsingException {
        if (string == null || string.isEmpty()) {
            throw new VersionParsingException("Version must be a non-empty string!");
        }
        try {
            return BridgeSemanticVersion.parse(string, false);
        } catch (VersionParsingException ignored) {
            // Fabric treats non-semantic values as opaque versions.
        }
        return new Version() {
            @Override public String getFriendlyString() { return string; }
            @Override public int compareTo(Version other) {
                return string.compareTo(other.getFriendlyString());
            }
            @Override public String toString() { return string; }
            @Override public boolean equals(Object value) {
                return value instanceof Version other
                        && !(other instanceof SemanticVersion)
                        && string.equals(other.getFriendlyString());
            }
            @Override public int hashCode() { return string.hashCode(); }
        };
    }
}
