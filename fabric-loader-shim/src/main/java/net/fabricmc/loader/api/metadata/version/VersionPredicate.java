package net.fabricmc.loader.api.metadata.version;

import dev.loaderbridge.fabric.runtime.BridgeVersionPredicate;
import java.util.Collection;
import java.util.function.Predicate;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;

public interface VersionPredicate extends Predicate<Version> {
    Collection<? extends PredicateTerm> getTerms();
    VersionInterval getInterval();

    interface PredicateTerm {
        VersionComparisonOperator getOperator();
        Version getReferenceVersion();
    }

    static VersionPredicate parse(String predicate) throws VersionParsingException {
        return BridgeVersionPredicate.parse(predicate);
    }

    static Collection<VersionPredicate> parse(Collection<String> predicates)
            throws VersionParsingException {
        return BridgeVersionPredicate.parse(predicates);
    }
}
