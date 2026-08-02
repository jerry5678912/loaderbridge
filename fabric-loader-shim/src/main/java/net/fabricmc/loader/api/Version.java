package net.fabricmc.loader.api;

public interface Version extends Comparable<Version> {
    String getFriendlyString();

    static Version parse(String string) throws VersionParsingException {
        if (string == null || string.isBlank()) {
            throw new VersionParsingException("Version must not be blank");
        }
        return new Version() {
            @Override public String getFriendlyString() { return string; }
            @Override public int compareTo(Version other) {
                return string.compareTo(other.getFriendlyString());
            }
            @Override public String toString() { return string; }
        };
    }
}
