package dev.loaderbridge.fabric.runtime;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;

/** Fabric extended-semver value used by the public Loader API shim. */
public final class BridgeSemanticVersion implements SemanticVersion {
    private static final Pattern IDENTIFIERS = Pattern.compile("|[-0-9A-Za-z]+(\\.[-0-9A-Za-z]+)*");
    private static final Pattern NUMERIC = Pattern.compile("0|[1-9][0-9]*");
    private final int[] components;
    private final String prerelease;
    private final String build;
    private final String friendly;

    private BridgeSemanticVersion(int[] components, String prerelease, String build) {
        this.components = components;
        this.prerelease = prerelease;
        this.build = build;
        this.friendly = format();
    }

    public static BridgeSemanticVersion parse(String input, boolean allowWildcard)
            throws VersionParsingException {
        if (input == null || input.isEmpty()) {
            throw new VersionParsingException("Version must be a non-empty string!");
        }
        String value = input;
        String build = null;
        int buildAt = value.indexOf('+');
        if (buildAt >= 0) {
            build = value.substring(buildAt + 1);
            value = value.substring(0, buildAt);
        }
        String prerelease = null;
        int dashAt = value.indexOf('-');
        if (dashAt >= 0) {
            prerelease = value.substring(dashAt + 1);
            value = value.substring(0, dashAt);
        }
        if (prerelease != null && !IDENTIFIERS.matcher(prerelease).matches()) {
            throw new VersionParsingException("Invalid prerelease string '" + prerelease + "'!");
        }
        if (value.startsWith(".") || value.endsWith(".")) {
            throw new VersionParsingException("Missing version component!");
        }
        String[] pieces = value.split("\\.", -1);
        int[] parsed = new int[pieces.length];
        int firstWildcard = -1;
        for (int index = 0; index < pieces.length; index++) {
            String piece = pieces[index];
            boolean wildcard = piece.equals("x") || piece.equals("X") || piece.equals("*");
            if (allowWildcard && wildcard) {
                if (prerelease != null || index == 0) {
                    throw new VersionParsingException("Invalid wildcard semantic version '" + input + "'");
                }
                parsed[index] = COMPONENT_WILDCARD;
                if (firstWildcard < 0) firstWildcard = index;
            } else {
                if (firstWildcard >= 0) {
                    throw new VersionParsingException("Interjacent wildcard versions are disallowed!");
                }
                try {
                    parsed[index] = Integer.parseInt(piece);
                    if (parsed[index] < 0) throw new NumberFormatException();
                } catch (NumberFormatException exception) {
                    throw new VersionParsingException(
                            "Could not parse version number component '" + piece + "'!", exception);
                }
            }
        }
        if (firstWildcard > 0) parsed = Arrays.copyOf(parsed, firstWildcard + 1);
        return new BridgeSemanticVersion(parsed, prerelease, build);
    }

    public static BridgeSemanticVersion of(int... components) {
        return new BridgeSemanticVersion(components.clone(), null, null);
    }

    @Override public int getVersionComponentCount() { return components.length; }

    @Override
    public int getVersionComponent(int position) {
        if (position < 0) throw new RuntimeException("Tried to access negative version number component!");
        if (position >= components.length) {
            return hasWildcard() ? COMPONENT_WILDCARD : 0;
        }
        return components[position];
    }

    @Override public Optional<String> getPrereleaseKey() { return Optional.ofNullable(prerelease); }
    @Override public Optional<String> getBuildKey() { return Optional.ofNullable(build); }
    @Override public boolean hasWildcard() { return components[components.length - 1] == COMPONENT_WILDCARD; }
    @Override public String getFriendlyString() { return friendly; }
    @Override public String toString() { return friendly; }

    @Override
    public int compareTo(Version other) {
        if (!(other instanceof SemanticVersion semantic)) {
            return friendly.compareTo(other.getFriendlyString());
        }
        int count = Math.max(components.length, semantic.getVersionComponentCount());
        for (int index = 0; index < count; index++) {
            int left = getVersionComponent(index);
            int right = semantic.getVersionComponent(index);
            if (left == COMPONENT_WILDCARD || right == COMPONENT_WILDCARD) continue;
            int result = Integer.compare(left, right);
            if (result != 0) return result;
        }
        return comparePrerelease(prerelease, semantic.getPrereleaseKey().orElse(null),
                hasWildcard(), semantic.hasWildcard());
    }

    private static int comparePrerelease(String left, String right, boolean leftWildcard,
            boolean rightWildcard) {
        if (left == null && right == null) return 0;
        if (left == null) return leftWildcard ? 0 : 1;
        if (right == null) return rightWildcard ? 0 : -1;
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        for (int index = 0; index < Math.max(leftParts.length, rightParts.length); index++) {
            if (index >= leftParts.length) return -1;
            if (index >= rightParts.length) return 1;
            String a = leftParts[index];
            String b = rightParts[index];
            boolean aNumber = NUMERIC.matcher(a).matches();
            boolean bNumber = NUMERIC.matcher(b).matches();
            if (aNumber != bNumber) return aNumber ? -1 : 1;
            int result = aNumber ? compareNumericText(a, b) : a.compareTo(b);
            if (result != 0) return result;
        }
        return 0;
    }

    private static int compareNumericText(String left, String right) {
        int length = Integer.compare(left.length(), right.length());
        return length != 0 ? length : left.compareTo(right);
    }

    private String format() {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < components.length; index++) {
            if (index > 0) result.append('.');
            result.append(components[index] == COMPONENT_WILDCARD ? "x" : components[index]);
        }
        if (prerelease != null) result.append('-').append(prerelease);
        if (build != null) result.append('+').append(build);
        return result.toString();
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof BridgeSemanticVersion other
                && Arrays.equals(components, other.components)
                && Objects.equals(prerelease, other.prerelease)
                && Objects.equals(build, other.build);
    }

    @Override public int hashCode() { return Objects.hash(Arrays.hashCode(components), prerelease, build); }
}
