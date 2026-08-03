package dev.loaderbridge.fabric.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionComparisonOperator;
import net.fabricmc.loader.api.metadata.version.VersionInterval;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import org.junit.jupiter.api.Test;

class BridgeVersionApiTest {
    @Test
    void parsesAndOrdersFabricExtendedSemanticVersions() throws Exception {
        SemanticVersion release = SemanticVersion.parse("1.21.1+build.7");
        SemanticVersion prerelease = SemanticVersion.parse("1.21.1-rc.1");

        assertThat(release.getVersionComponentCount()).isEqualTo(3);
        assertThat(release.getVersionComponent(4)).isZero();
        assertThat(release.getBuildKey()).contains("build.7");
        assertThat(prerelease.getPrereleaseKey()).contains("rc.1");
        assertThat(release).isGreaterThan(prerelease);
        assertThat(Version.parse("release-candidate")).isNotInstanceOf(SemanticVersion.class);
        assertThatThrownBy(() -> SemanticVersion.parse("1.x"))
                .isInstanceOf(VersionParsingException.class);
    }

    @Test
    void exposesFabricPredicateTermsAndIntervals() throws Exception {
        VersionPredicate predicate = VersionPredicate.parse(">=1.20 <1.22");

        assertThat(predicate.test(Version.parse("1.21.1"))).isTrue();
        assertThat(predicate.test(Version.parse("1.22.0"))).isFalse();
        assertThat(predicate.getTerms())
                .extracting(VersionPredicate.PredicateTerm::getOperator)
                .containsExactly(VersionComparisonOperator.GREATER_EQUAL,
                        VersionComparisonOperator.LESS);
        assertThat(predicate.getInterval().getMin().getFriendlyString()).isEqualTo("1.20");
        assertThat(predicate.getInterval().getMax().getFriendlyString()).isEqualTo("1.22");
        assertThat(predicate.getInterval().isMaxInclusive()).isFalse();
    }

    @Test
    void convertsWildcardsAndComputesIntervalOperations() throws Exception {
        VersionPredicate wildcard = VersionPredicate.parse("1.21.x");
        assertThat(wildcard.test(Version.parse("1.21.4"))).isTrue();
        assertThat(wildcard.test(Version.parse("1.22.0"))).isFalse();

        VersionInterval selected = wildcard.getInterval();
        assertThat(selected.getMin().getFriendlyString()).isEqualTo("1.21-");
        assertThat(selected.getMax().getFriendlyString()).isEqualTo("1.22-");
        assertThat(selected.not()).hasSize(2);
        assertThat(VersionInterval.or(List.of(selected), selected)).containsExactly(selected);
        assertThat(VersionInterval.or(List.of(VersionInterval.INFINITE), selected))
                .containsExactly(VersionInterval.INFINITE);
    }
}
