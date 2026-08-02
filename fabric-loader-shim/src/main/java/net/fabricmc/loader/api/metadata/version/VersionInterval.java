package net.fabricmc.loader.api.metadata.version;

import dev.loaderbridge.fabric.runtime.BridgeVersionInterval;
import java.util.Collection;
import java.util.List;
import net.fabricmc.loader.api.Version;

public interface VersionInterval {
    VersionInterval INFINITE = new BridgeVersionInterval(null, false, null, false);
    boolean isSemantic();
    Version getMin();
    boolean isMinInclusive();
    Version getMax();
    boolean isMaxInclusive();

    default VersionInterval and(VersionInterval other) { return and(this, other); }
    default List<VersionInterval> or(Collection<VersionInterval> others) { return or(others, this); }
    default List<VersionInterval> not() { return not(this); }

    static VersionInterval and(VersionInterval left, VersionInterval right) {
        return BridgeVersionInterval.intersect(left, right);
    }
    static List<VersionInterval> and(Collection<VersionInterval> left,
            Collection<VersionInterval> right) {
        return BridgeVersionInterval.intersect(left, right);
    }
    static List<VersionInterval> or(Collection<VersionInterval> intervals, VersionInterval added) {
        return BridgeVersionInterval.union(intervals, added);
    }
    static List<VersionInterval> not(VersionInterval interval) {
        return BridgeVersionInterval.complement(interval);
    }
    static List<VersionInterval> not(Collection<VersionInterval> intervals) {
        return BridgeVersionInterval.complement(intervals);
    }
}
