package net.fabricmc.loader.api;

import dev.loaderbridge.fabric.runtime.BridgeSemanticVersion;
import java.util.Optional;

public interface SemanticVersion extends Version {
    int COMPONENT_WILDCARD = Integer.MIN_VALUE;

    int getVersionComponentCount();
    int getVersionComponent(int position);
    Optional<String> getPrereleaseKey();
    Optional<String> getBuildKey();
    boolean hasWildcard();

    /** @deprecated Use {@link #compareTo(Version)}. */
    @Deprecated
    default int compareTo(SemanticVersion other) {
        return compareTo((Version) other);
    }

    static SemanticVersion parse(String value) throws VersionParsingException {
        return BridgeSemanticVersion.parse(value, false);
    }
}
