package dev.loaderbridge.fabric.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.version.VersionInterval;

public record BridgeVersionInterval(Version min, boolean minInclusive, Version max,
        boolean maxInclusive) implements VersionInterval {
    public BridgeVersionInterval {
        if (min == null) minInclusive = false;
        if (max == null) maxInclusive = false;
    }
    @Override public boolean isSemantic() {
        return (min == null || min instanceof SemanticVersion)
                && (max == null || max instanceof SemanticVersion);
    }
    @Override public Version getMin() { return min; }
    @Override public boolean isMinInclusive() { return minInclusive; }
    @Override public Version getMax() { return max; }
    @Override public boolean isMaxInclusive() { return maxInclusive; }

    public static VersionInterval intersect(VersionInterval left, VersionInterval right) {
        if (left == null || right == null) return null;
        if (!left.isSemantic() || !right.isSemantic()) return intersectPlain(left, right);
        Bound lower = later(left.getMin(), left.isMinInclusive(), right.getMin(), right.isMinInclusive());
        Bound upper = earlier(left.getMax(), left.isMaxInclusive(), right.getMax(), right.isMaxInclusive());
        if (lower.version != null && upper.version != null) {
            int comparison = lower.version.compareTo(upper.version);
            if (comparison > 0 || (comparison == 0 && (!lower.inclusive || !upper.inclusive))) return null;
        }
        return new BridgeVersionInterval(lower.version, lower.inclusive, upper.version, upper.inclusive);
    }

    private static VersionInterval intersectPlain(VersionInterval left, VersionInterval right) {
        Version leftMin = left.getMin();
        Version leftMax = left.getMax();
        Version rightMin = right.getMin();
        Version rightMax = right.getMax();
        if (leftMin != null) {
            if (rightMin != null && !leftMin.equals(rightMin)
                    || rightMax != null && !leftMin.equals(rightMax)) return null;
            if (leftMax != null || rightMax == null) return left;
            return new BridgeVersionInterval(leftMin, true, rightMax, right.isMaxInclusive());
        } else if (leftMax != null) {
            if (rightMin != null && !leftMax.equals(rightMin)
                    || rightMax != null && !leftMax.equals(rightMax)) return null;
            if (rightMin == null) return left;
            if (rightMax != null) return right;
            return new BridgeVersionInterval(rightMin, true, leftMax, true);
        }
        return right;
    }

    public static List<VersionInterval> intersect(Collection<VersionInterval> left,
            Collection<VersionInterval> right) {
        List<VersionInterval> result = new ArrayList<>();
        for (VersionInterval a : left) for (VersionInterval b : right) {
            VersionInterval overlap = intersect(a, b);
            if (overlap != null) result = new ArrayList<>(union(result, overlap));
        }
        return List.copyOf(result);
    }

    public static List<VersionInterval> union(Collection<VersionInterval> intervals,
            VersionInterval added) {
        if ((added != null && !added.isSemantic())
                || intervals.stream().anyMatch(interval -> interval != null && !interval.isSemantic())) {
            List<VersionInterval> result = new ArrayList<>();
            for (VersionInterval interval : intervals) mergePlainCompatible(interval, result);
            mergePlainCompatible(added, result);
            return List.copyOf(result);
        }
        List<VersionInterval> sorted = new ArrayList<>(intervals);
        if (added != null) sorted.add(added);
        sorted.sort(Comparator.comparing(VersionInterval::getMin,
                Comparator.nullsFirst(Version::compareTo))
                .thenComparing(interval -> !interval.isMinInclusive()));
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

    private static void mergePlainCompatible(VersionInterval interval,
            List<VersionInterval> result) {
        if (interval == null) return;
        if (result.size() == 1 && result.getFirst().getMin() == null
                && result.getFirst().getMax() == null) return;
        if (interval.getMin() == null && interval.getMax() == null) {
            result.clear();
            result.add(VersionInterval.INFINITE);
            return;
        }
        if (interval.isSemantic()) {
            List<VersionInterval> semantic = result.stream()
                    .filter(VersionInterval::isSemantic).toList();
            result.removeIf(VersionInterval::isSemantic);
            result.addAll(union(semantic, interval));
            return;
        }
        Version min = interval.getMin();
        Version max = interval.getMax();
        Version value = min != null ? min : max;
        for (int index = 0; index < result.size(); index++) {
            VersionInterval current = result.get(index);
            if (value.equals(current.getMin())) {
                if (min == null) {
                    result.clear();
                    result.add(VersionInterval.INFINITE);
                } else if (max == null && current.getMax() != null) {
                    result.set(index, interval);
                }
                return;
            } else if (value.equals(current.getMax())) {
                if (max == null) {
                    result.clear();
                    result.add(VersionInterval.INFINITE);
                }
                return;
            }
        }
        result.add(interval);
    }

    public static List<VersionInterval> complement(VersionInterval interval) {
        if (interval == null) return List.of(VersionInterval.INFINITE);
        if (interval.getMin() == null && interval.getMax() == null) return List.of();
        if (interval.getMin() != null && interval.getMax() != null
                && interval.getMin().equals(interval.getMax())
                && !interval.isMinInclusive() && !interval.isMaxInclusive()) {
            return List.of(VersionInterval.INFINITE);
        }
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

    @Override public boolean equals(Object other) {
        return other instanceof VersionInterval interval
                && Objects.equals(min, interval.getMin())
                && minInclusive == interval.isMinInclusive()
                && Objects.equals(max, interval.getMax())
                && maxInclusive == interval.isMaxInclusive();
    }

    @Override public int hashCode() {
        return (Objects.hashCode(min) + (minInclusive ? 1 : 0)) * 31
                + (Objects.hashCode(max) + (maxInclusive ? 1 : 0)) * 31;
    }

    @Override public String toString() {
        if (min == null) {
            return max == null ? "(-∞,∞)" : "(-∞," + max + (maxInclusive ? ']' : ')');
        }
        if (max == null) return (minInclusive ? '[' : '(') + min.toString() + ",∞)";
        return (minInclusive ? '[' : '(') + min.toString() + ',' + max
                + (maxInclusive ? ']' : ')');
    }

    private record Bound(Version version, boolean inclusive) {}
}
