package dev.loaderbridge.fabric.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Common Fabric extended-semver predicates used by 1.21.1 metadata. */
public final class FabricVersionPredicate {
    private FabricVersionPredicate() {}

    public static boolean anyMatches(List<String> predicates, String version) {
        return predicates.stream().anyMatch(predicate -> matches(predicate, version));
    }

    /** Compares versions using the same extended-semver ordering as predicates. */
    public static int compare(String left, String right) {
        return ComparableVersion.parse(left).compareTo(ComparableVersion.parse(right));
    }

    public static boolean matches(String expression, String version) {
        String trimmed = expression.trim();
        if (trimmed.isEmpty() || trimmed.equals("*")) {
            return true;
        }
        for (String predicate : trimmed.split("\\s+")) {
            if (!matchesSingle(predicate, version)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesSingle(String predicate, String version) {
        if (predicate.equals("*")) {
            return true;
        }

        String lower = predicate.toLowerCase(Locale.ROOT);
        if (lower.contains(".x") || lower.contains(".*")) {
            String prefix = lower.replace("*", "x");
            prefix = prefix.substring(0, prefix.indexOf('x'));
            return version.toLowerCase(Locale.ROOT).startsWith(prefix);
        }

        String operator;
        String expected;
        if (predicate.startsWith(">=") || predicate.startsWith("<=")) {
            operator = predicate.substring(0, 2);
            expected = predicate.substring(2);
        } else if (!predicate.isEmpty() && "><=~^".indexOf(predicate.charAt(0)) >= 0) {
            operator = predicate.substring(0, 1);
            expected = predicate.substring(1);
        } else {
            operator = "=";
            expected = predicate;
        }

        ComparableVersion actualVersion = ComparableVersion.parse(version);
        ComparableVersion expectedVersion = ComparableVersion.parse(expected);
        int comparison = actualVersion.compareTo(expectedVersion);
        return switch (operator) {
            case ">=" -> comparison >= 0;
            case ">" -> comparison > 0;
            case "<=" -> comparison <= 0;
            case "<" -> comparison < 0;
            case "~" -> comparison >= 0 && actualVersion.compareTo(expectedVersion.nextMinor()) < 0;
            case "^" -> comparison >= 0 && actualVersion.compareTo(expectedVersion.nextCompatible()) < 0;
            default -> comparison == 0;
        };
    }

    private record ComparableVersion(List<Integer> numbers, String suffix, String original)
            implements Comparable<ComparableVersion> {
        static ComparableVersion parse(String value) {
            String comparisonValue = value.trim();
            int buildSeparator = comparisonValue.indexOf('+');
            if (buildSeparator >= 0) comparisonValue = comparisonValue.substring(0, buildSeparator);
            String[] mainAndSuffix = comparisonValue.split("-", 2);
            String[] pieces = mainAndSuffix[0].split("\\.");
            List<Integer> numbers = new ArrayList<>();
            for (String piece : pieces) {
                if (!piece.matches("[0-9]+")) {
                    return new ComparableVersion(List.of(), "", value);
                }
                try {
                    numbers.add(Integer.parseInt(piece));
                } catch (NumberFormatException exception) {
                    return new ComparableVersion(List.of(), "", value);
                }
            }
            String suffix = mainAndSuffix.length == 2 ? mainAndSuffix[1] : "";
            return new ComparableVersion(List.copyOf(numbers), suffix, value);
        }

        ComparableVersion nextMinor() {
            if (numbers.isEmpty()) {
                return this;
            }
            int major = number(0);
            int minor = number(1);
            if (numbers.size() == 1) {
                return numeric(major + 1, 0, 0);
            }
            return numeric(major, minor + 1, 0);
        }

        ComparableVersion nextCompatible() {
            if (numbers.isEmpty()) {
                return this;
            }
            int major = number(0);
            int minor = number(1);
            int patch = number(2);
            if (major > 0) {
                return numeric(major + 1, 0, 0);
            }
            if (minor > 0) {
                return numeric(0, minor + 1, 0);
            }
            return numeric(0, 0, patch + 1);
        }

        private int number(int index) {
            return index < numbers.size() ? numbers.get(index) : 0;
        }

        private static ComparableVersion numeric(int major, int minor, int patch) {
            return new ComparableVersion(List.of(major, minor, patch), "", major + "." + minor + "." + patch);
        }

        @Override
        public int compareTo(ComparableVersion other) {
            if (numbers.isEmpty() || other.numbers.isEmpty()) {
                return original.compareTo(other.original);
            }
            int size = Math.max(numbers.size(), other.numbers.size());
            for (int index = 0; index < size; index++) {
                int comparison = Integer.compare(number(index), other.number(index));
                if (comparison != 0) {
                    return comparison;
                }
            }
            if (suffix.isEmpty() != other.suffix.isEmpty()) {
                return suffix.isEmpty() ? 1 : -1;
            }
            return suffix.compareTo(other.suffix);
        }
    }
}
