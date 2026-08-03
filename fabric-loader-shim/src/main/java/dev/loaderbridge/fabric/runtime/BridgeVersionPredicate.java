package dev.loaderbridge.fabric.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionComparisonOperator;
import net.fabricmc.loader.api.metadata.version.VersionInterval;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;

public final class BridgeVersionPredicate implements VersionPredicate {
    private final List<Term> terms;

    private BridgeVersionPredicate(List<Term> terms) {
        this.terms = List.copyOf(terms);
    }

    public static VersionPredicate parse(String expression) throws VersionParsingException {
        Objects.requireNonNull(expression, "predicate");
        List<Term> terms = new ArrayList<>();
        for (String token : expression.split(" ")) {
            if (token.isBlank() || token.equals("*")) continue;
            VersionComparisonOperator operator = VersionComparisonOperator.EQUAL;
            for (VersionComparisonOperator candidate : VersionComparisonOperator.values()) {
                if (token.startsWith(candidate.getSerialized())) {
                    operator = candidate;
                    token = token.substring(candidate.getSerialized().length());
                    break;
                }
            }
            Version version;
            try {
                version = BridgeSemanticVersion.parse(token, true);
            } catch (VersionParsingException exception) {
                version = Version.parse(token);
            }
            if (version instanceof SemanticVersion semantic && semantic.hasWildcard()) {
                if (operator != VersionComparisonOperator.EQUAL) {
                    throw new VersionParsingException("Wildcard ranges require equality: " + expression);
                }
                int wildcard = semantic.getVersionComponentCount() - 1;
                int[] components = new int[wildcard];
                for (int index = 0; index < wildcard; index++) {
                    components[index] = semantic.getVersionComponent(index);
                }
                version = BridgeSemanticVersion.rangeMinimum(
                        semantic.getBuildKey().orElse(null), components);
                operator = wildcard == 1 ? VersionComparisonOperator.SAME_TO_NEXT_MAJOR
                        : VersionComparisonOperator.SAME_TO_NEXT_MINOR;
            } else if (!(version instanceof SemanticVersion)
                    && !operator.isMinInclusive() && !operator.isMaxInclusive()) {
                throw new VersionParsingException(
                        "Exclusive ranges require semantic versions: " + expression);
            } else if (!(version instanceof SemanticVersion)) {
                operator = VersionComparisonOperator.EQUAL;
            }
            terms.add(new Term(operator, version));
        }
        return new BridgeVersionPredicate(terms);
    }

    public static Collection<VersionPredicate> parse(Collection<String> expressions)
            throws VersionParsingException {
        Collection<VersionPredicate> result = new LinkedHashSet<>();
        for (String expression : expressions) result.add(parse(expression));
        return result;
    }

    @Override
    public boolean test(Version version) {
        Objects.requireNonNull(version, "null version");
        return terms.stream().allMatch(term -> term.test(version));
    }

    @Override public Collection<? extends PredicateTerm> getTerms() { return terms; }

    @Override
    public VersionInterval getInterval() {
        VersionInterval result = VersionInterval.INFINITE;
        for (Term term : terms) result = VersionInterval.and(result, term.interval());
        return result;
    }

    @Override public String toString() {
        return terms.isEmpty() ? "*" : String.join(" ", terms.stream().map(Term::toString).toList());
    }

    private record Term(VersionComparisonOperator operator, Version referenceVersion)
            implements PredicateTerm {
        @Override public VersionComparisonOperator getOperator() { return operator; }
        @Override public Version getReferenceVersion() { return referenceVersion; }
        boolean test(Version version) { return operator.test(version, referenceVersion); }
        VersionInterval interval() {
            if (referenceVersion instanceof SemanticVersion semantic) {
                return new BridgeVersionInterval(operator.minVersion(semantic),
                        operator.isMinInclusive(), operator.maxVersion(semantic),
                        operator.isMaxInclusive());
            }
            return new BridgeVersionInterval(referenceVersion, true, referenceVersion, true);
        }
        @Override public String toString() { return operator.getSerialized() + referenceVersion; }
    }
}
