package net.fabricmc.loader.api.metadata.version;

import java.util.Collection;
import java.util.function.Predicate;
import net.fabricmc.loader.api.Version;

public interface VersionPredicate extends Predicate<Version> {
    Collection<? extends PredicateTerm> getTerms();
    VersionInterval getInterval();

    interface PredicateTerm extends Predicate<Version> {}
}
