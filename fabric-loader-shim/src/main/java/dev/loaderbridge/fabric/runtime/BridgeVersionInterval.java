package dev.loaderbridge.fabric.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.version.VersionInterval;

public record BridgeVersionInterval(Version min, boolean minInclusive, Version max,
        boolean maxInclusive) implements VersionInterval {
    @Override public boolean isSemantic() {
        return (min == null || min instanceof SemanticVersion)
                && (max == null || max instanceof SemanticVersion);
    }
    @Override public Version getMin() { return min; }
    @Override public boolean isMinInclusive() { return minInclusive; }
    @Override public Version getMax() { return max; }
    @Override public boolean isMaxInclusive() { return maxInclusive; }

    public static VersionInterval intersect(VersionInterval left, VersionInterval right) {
        Bound lower = later(left.getMin(), left.isMinInclusive(), right.getMin(), right.isMinInclusive());
        Bound upper = earlier(left.getMax(), left.isMaxInclusive(), right.getMax(), right.isMaxInclusive());
        if (lower.version != null && upper.version != null) {
            int comparison = lower.version.compareTo(upper.version);
            if (comparison > 0 || (comparison == 0 && (!lower.inclusive || !upper.inclusive))) return null;
        }
        return new BridgeVersionInterval(lower.version, lower.inclusive, upper.version, upper.inclusive);
    }

    public static List<VersionInterval> intersect(Collection<VersionInterval> left,
            Collection<VersionInterval> right) {
        List<VersionInterval> result = new ArrayList<>();
        for (VersionInterval a : left) for (VersionInterval b : right) {
            VersionInterval overlap = intersect(a, b);
            if (overlap != null) result.add(overlap);
        }
        return List.copyOf(result);
    }

    public static List<VersionInterval> union(Collection<VersionInterval> intervals,
            VersionInterval added) {
        List<VersionInterval> sorted = new ArrayList<>(intervals);
        sorted.add(added);
        sorted.sort(Comparator.comparing(VersionInterval::getMin,
                Comparator.nullsFirst(Version::compareTo)));
        List<VersionInterval> result = new ArrayList<>();
        for (VersionInterval current : sorted) {
            if (result.isEmpty()) {
                result.add(current);
                continue;
            }
            VersionInterval previous = result.getLast();
            if (previous.getMax() == null || current.getMin() == null
                    || previous.getMax().compareTo(current.getMin()) > 0
                    || (previous.getMax().compareTo(current.getMin()) == 0
                            && (previous.isMaxInclusive() || current.isMinInclusive()))) {
                Bound upper = laterUpper(previous.getMax(), previous.isMaxInclusive(),
                        current.getMax(), current.isMaxInclusive());
                result.set(result.size() - 1, new BridgeVersionInterval(previous.getMin(),
                        previous.isMinInclusive(), upper.version, upper.inclusive));
            } else {
                result.add(current);
            }
        }
        return List.copyOf(result);
    }

    public static List<VersionInterval> complement(VersionInterval interval) {
        if (interval == null) return List.of(VersionInterval.INFINITE);
        List<VersionInterval> result = new ArrayList<>(2);
        if (interval.getMin() != null) result.add(new BridgeVersionInterval(
                null, false, interval.getMin(), !interval.isMinInclusive()));
        if (interval.getMax() != null) result.add(new BridgeVersionInterval(
                interval.getMax(), !interval.isMaxInclusive(), null, false));
        return List.copyOf(result);
    }

    public static List<VersionInterval> complement(Collection<VersionInterval> intervals) {
        List<VersionInterval> result = List.of(VersionInterval.INFINITE);
        for (VersionInterval interval : intervals) result = intersect(result, complement(interval));
        return result;
    }

    private static Bound later(Version left, boolean leftInclusive, Version right,
            boolean rightInclusive) {
        if (left == null) return new Bound(right, rightInclusive);
        if (right == null) return new Bound(left, leftInclusive);
        int comparison = left.compareTo(right);
        if (comparison > 0) return new Bound(left, leftInclusive);
        if (comparison < 0) return new Bound(right, rightInclusive);
        return new Bound(left, leftInclusive && rightInclusive);
    }

    private static Bound earlier(Version left, boolean leftInclusive, Version right,
            boolean rightInclusive) {
        if (left == null) return new Bound(right, rightInclusive);
        if (right == null) return new Bound(left, leftInclusive);
        int comparison = left.compareTo(right);
        if (comparison < 0) return new Bound(left, leftInclusive);
        if (comparison > 0) return new Bound(right, rightInclusive);
        return new Bound(left, leftInclusive && rightInclusive);
    }

    private static Bound laterUpper(Version left, boolean leftInclusive, Version right,
            boolean rightInclusive) {
        if (left == null || right == null) return new Bound(null, false);
        int comparison = left.compareTo(right);
        if (comparison > 0) return new Bound(left, leftInclusive);
        if (comparison < 0) return new Bound(right, rightInclusive);
        return new Bound(left, leftInclusive || rightInclusive);
    }

    private record Bound(Version version, boolean inclusive) {}
}
