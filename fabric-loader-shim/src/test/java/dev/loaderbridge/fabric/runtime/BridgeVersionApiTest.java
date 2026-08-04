package dev.loaderbridge.fabric.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
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

        SemanticVersion shortVersion = SemanticVersion.parse("1");
        SemanticVersion paddedVersion = SemanticVersion.parse("1.0.0");
        assertThat(shortVersion.compareTo((Version) paddedVersion)).isZero();
        assertThat(shortVersion).isEqualTo(paddedVersion).hasSameHashCodeAs(paddedVersion);
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
    void givesParsedPredicatesFabricCompatibleValueEquality() throws Exception {
        VersionPredicate first = VersionPredicate.parse(">=1.20 <1.22");
        VersionPredicate second = VersionPredicate.parse(">=1.20 <1.22");

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
        assertThat(VersionPredicate.parse(List.of(">=1.20", ">=1.20")))
                .containsExactlyElementsOf(Set.of(VersionPredicate.parse(">=1.20")));
        assertThat(VersionPredicate.parse(">=1"))
                .isEqualTo(VersionPredicate.parse(">=1.0.0"))
                .hasSameHashCodeAs(VersionPredicate.parse(">=1.0.0"));
        assertThat(VersionPredicate.parse(List.of(">=1", ">=1.0.0"))).hasSize(1);
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
        assertThat(VersionInterval.and(List.of(selected, selected), List.of(selected)))
                .containsExactly(selected);
        Version one = Version.parse("1");
        Version two = Version.parse("2");
        VersionInterval open = new BridgeVersionInterval(one, false, two, false);
        VersionInterval leftPoint = new BridgeVersionInterval(one, true, one, true);
        assertThat(VersionInterval.or(List.of(open), leftPoint)).singleElement()
                .satisfies(union -> {
                    assertThat(union.getMin()).isEqualTo(one);
                    assertThat(union.isMinInclusive()).isTrue();
                    assertThat(union.getMax()).isEqualTo(two);
                    assertThat(union.isMaxInclusive()).isFalse();
                });
    }

    @Test
    void givesIntervalsFabricCompatibleCrossImplementationValueSemantics() throws Exception {
        Version min = Version.parse("1.20");
        Version max = Version.parse("1.22");
        VersionInterval interval = new BridgeVersionInterval(min, true, max, false);
        VersionInterval equivalent = new VersionInterval() {
            @Override public boolean isSemantic() { return true; }
            @Override public Version getMin() { return min; }
            @Override public boolean isMinInclusive() { return true; }
            @Override public Version getMax() { return max; }
            @Override public boolean isMaxInclusive() { return false; }
        };

        assertThat(interval).isEqualTo(equivalent);
        assertThat(interval.toString()).isEqualTo("[1.20,1.22)");
        assertThat(new BridgeVersionInterval(null, true, max, false).isMinInclusive()).isFalse();
        assertThat(new BridgeVersionInterval(min, true, null, true).isMaxInclusive()).isFalse();
    }

    @Test
    void preservesFabricPlainVersionIntervalRules() throws Exception {
        VersionInterval point = VersionPredicate.parse("release-candidate").getInterval();
        List<VersionInterval> complement = point.not();

        assertThat(point.isSemantic()).isFalse();
        assertThat(point.toString()).isEqualTo("[release-candidate,release-candidate]");
        assertThat(complement).extracting(Object::toString)
                .containsExactly("(-∞,release-candidate)", "(release-candidate,∞)");
        assertThat(VersionInterval.or(List.of(complement.getFirst()), complement.getLast()))
                .containsExactly(VersionInterval.INFINITE);
    }
}
