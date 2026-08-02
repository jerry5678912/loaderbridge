package net.fabricmc.loader.api.metadata.version;

import net.fabricmc.loader.api.Version;

public interface VersionInterval {
    boolean isSemantic();
    Version getMin();
    boolean isMinInclusive();
    Version getMax();
    boolean isMaxInclusive();
}
