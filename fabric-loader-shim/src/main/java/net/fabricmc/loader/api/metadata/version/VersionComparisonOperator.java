package net.fabricmc.loader.api.metadata.version;

import dev.loaderbridge.fabric.runtime.BridgeSemanticVersion;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;

public enum VersionComparisonOperator {
    GREATER_EQUAL(">=", true, false),
    LESS_EQUAL("<=", false, true),
    GREATER(">", false, false),
    LESS("<", false, false),
    EQUAL("=", true, true),
    SAME_TO_NEXT_MINOR("~", true, false),
    SAME_TO_NEXT_MAJOR("^", true, false);

    private final String serialized;
    private final boolean minInclusive;
    private final boolean maxInclusive;

    VersionComparisonOperator(String serialized, boolean minInclusive, boolean maxInclusive) {
        this.serialized = serialized;
        this.minInclusive = minInclusive;
        this.maxInclusive = maxInclusive;
    }

    public String getSerialized() { return serialized; }
    public boolean isMinInclusive() { return minInclusive; }
    public boolean isMaxInclusive() { return maxInclusive; }

    public boolean test(Version actual, Version reference) {
        if (!(actual instanceof SemanticVersion a) || !(reference instanceof SemanticVersion b)) {
            return (minInclusive || maxInclusive)
                    && actual.getFriendlyString().equals(reference.getFriendlyString());
        }
        int comparison = a.compareTo((Version) b);
        return switch (this) {
            case GREATER_EQUAL -> comparison >= 0;
            case LESS_EQUAL -> comparison <= 0;
            case GREATER -> comparison > 0;
            case LESS -> comparison < 0;
            case EQUAL -> comparison == 0;
            case SAME_TO_NEXT_MINOR -> comparison >= 0
                    && a.getVersionComponent(0) == b.getVersionComponent(0)
                    && a.getVersionComponent(1) == b.getVersionComponent(1);
            case SAME_TO_NEXT_MAJOR -> comparison >= 0
                    && a.getVersionComponent(0) == b.getVersionComponent(0);
        };
    }

    public boolean test(SemanticVersion actual, SemanticVersion reference) {
        return test((Version) actual, reference);
    }

    public SemanticVersion minVersion(SemanticVersion version) {
        return switch (this) {
            case GREATER_EQUAL, GREATER, EQUAL, SAME_TO_NEXT_MINOR, SAME_TO_NEXT_MAJOR -> version;
            default -> null;
        };
    }

    public SemanticVersion maxVersion(SemanticVersion version) {
        return switch (this) {
            case LESS_EQUAL, LESS, EQUAL -> version;
            case SAME_TO_NEXT_MINOR -> BridgeSemanticVersion.of(
                    version.getVersionComponent(0), version.getVersionComponent(1) + 1);
            case SAME_TO_NEXT_MAJOR -> BridgeSemanticVersion.of(version.getVersionComponent(0) + 1);
            default -> null;
        };
    }
}
