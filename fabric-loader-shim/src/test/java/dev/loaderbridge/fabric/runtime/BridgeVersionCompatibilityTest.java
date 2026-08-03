package dev.loaderbridge.fabric.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import org.junit.jupiter.api.Test;

/**
 * Compatibility matrix derived independently from Fabric Loader 0.16.14's Apache-2.0 tests.
 * https://github.com/FabricMC/fabric-loader/blob/0.16.14/src/test/java/net/fabricmc/test/VersionParsingTests.java
 */
class BridgeVersionCompatibilityTest {
    @Test
    void acceptsAndRejectsFabricExtendedSemanticVersions() {
        for (String value : List.of(
                "0.3.5", "0.3.5-beta.2", "0.3.5-alpha.6+build.120",
                "0.3.5+build.3000", "1.0.0-0.3.7", "1.0.0-x.7.z.92",
                "1.0.0+20130313144700", "1.0.0-beta+exp.sha.5114f85")) {
            assertThat(parseFailure(value)).as(value).isNull();
        }
        for (String value : List.of(
                "0.0.-1", "0.2147483648.0", "0.-1.0", "-1.0.0", "",
                "0.0.a", "0.a.0", "a.0.0", "2.x", "2.X", "2.*", "1.x.2")) {
            assertThat(parseFailure(value)).as(value).isInstanceOf(VersionParsingException.class);
        }
    }

    @Test
    void matchesFabricPrereleaseAndComparatorRanges() throws Exception {
        assertPredicate(">=0.3.1-beta.2 <0.4.0",
                List.of("0.3.1-beta.2", "0.3.1-beta.2.1", "0.3.1-beta.3",
                        "0.3.4+build.125", "0.3.7", "0.4.0-alpha.1",
                        "0.3.4-beta.7", "0.3.1-beta.11"),
                List.of("0.3.0", "0.3.1-beta.1", "0.4.0"));
        assertPredicate(">=0.3.1-beta.2 <0.4.0-",
                List.of("0.3.1-beta.2", "0.3.1-beta.2.1", "0.3.1-beta.3",
                        "0.3.4+build.125", "0.3.7", "0.3.4-beta.7", "0.3.1-beta.11"),
                List.of("0.3.0", "0.3.1-beta.1", "0.4.0-alpha.1", "0.4.0"));
        assertPredicate(">=1.4-",
                List.of("1.4-beta.2", "1.4+build.125", "1.4", "1.4.2"),
                List.of("1.3", "1.3.5", "1.3-alpha.1"));
        assertPredicate("<1.4",
                List.of("1.3", "1.3.5", "1.3-alpha.1", "1.4-beta.2"),
                List.of("1.4+build.125", "1.4"));
        assertPredicate("<1.4-",
                List.of("1.3", "1.3.5", "1.3-alpha.1"),
                List.of("1.4-beta.2", "1.4+build.125", "1.4"));
        assertPredicate(">=0.3.1-beta.8.d.10",
                List.of("0.3.1-beta.9", "0.3.1-beta.11", "0.3.1-beta.8.e",
                        "0.3.1-beta.8.d.10", "0.3.1-beta.9.d.5", "0.3.1-beta.final",
                        "0.3.1-beta.-final-"),
                List.of("0.3.1-beta.7", "0.3.1-beta.8.d", "0.3.1-beta.8.a",
                        "0.3.1-alpha.9", "0.3.1-beta.8.8"));
    }

    @Test
    void matchesFabricWildcardTildeAndCaretRanges() throws Exception {
        assertPredicate("1.3.x",
                List.of("1.3.0", "1.3.0-alpha.1", "1.3.99"),
                List.of("1.4.0", "1.2.9", "1.2.9-rc.6", "1.4.0-alpha.1", "2.0.0"));
        assertPredicate("2.x",
                List.of("2.0.0", "2.0.0-alpha.1", "2.9.0-beta.2", "2.2.4"),
                List.of("1.99.99", "3.0.0", "3.0.0-alpha.1"));
        assertPredicate("~1.2.3",
                List.of("1.2.3", "1.2.4", "1.2.4-alpha.1"),
                List.of("1.2.2", "1.2.3-rc.7", "1.3.0", "2.2.0"));
        assertPredicate("~1.2",
                List.of("1.2.0", "1.2.1-alpha.3", "1.2.6"),
                List.of("1.1.9", "1.3.0", "1.2.0-rc.2", "1.3.0-alpha.3"));
        assertPredicate("~1.2-",
                List.of("1.2.0", "1.2.1-alpha.3", "1.2.6", "1.2.0-rc.2"),
                List.of("1.1.9", "1.3.0", "1.3.0-alpha.3"));
        assertPredicate("~1",
                List.of("1.0.0", "1.0.4"),
                List.of("0.9.9", "1.1.5", "3.0.5"));
        assertPredicate("~1.2.3-beta.2",
                List.of("1.2.3-beta.2", "1.2.3-beta.2.1", "1.2.3-beta.3",
                        "1.2.3-beta.11", "1.2.3-rc.7", "1.2.3", "1.2.5",
                        "1.2.4-alpha.4"),
                List.of("1.3.0", "1.2.2", "1.2.3-beta.1", "1.2.3-beta.1.9",
                        "1.2.3-alpha.4"));
        assertPredicate("^1.2.3",
                List.of("1.2.3", "1.2.4", "1.3.0", "1.2.4-beta.2"),
                List.of("1.2.2", "1.2.3-beta.2", "2.0.0"));
        assertPredicate("^0.2.3",
                List.of("0.2.3", "0.2.4", "0.2.8-beta.2", "0.3.0"),
                List.of("0.2.0", "0.2.3-rc.8", "1.2.0"));
        assertPredicate("^1.2.3-beta.2",
                List.of("1.2.3-beta.2", "1.2.3-beta.3", "1.2.3-rc.7", "1.2.3",
                        "1.2.5", "1.3.0", "1.2.4-alpha.4"),
                List.of("1.2.2", "2.0.0", "1.2.3-alpha.4"));
        assertPredicate("^1",
                List.of("1.0.0", "1.2.4", "1.99.99", "1.2.4-beta.2"),
                List.of("0.9.6", "1.0.0-rc.5", "2.0.0", "2.0.0-beta.2"));
        assertPredicate("^1-",
                List.of("1.0.0", "1.0.0-rc.5", "1.2.4", "1.99.99", "1.2.4-beta.2"),
                List.of("0.9.0", "0.9.0-rc.5", "2.0.0", "2.0.0-beta.2"));
    }

    @Test
    void rejectsMalformedPredicateTerms() {
        assertThatThrownBy(() -> VersionPredicate.parse(">=1.2.x"))
                .isInstanceOf(VersionParsingException.class);
    }

    private static Exception parseFailure(String value) {
        try {
            SemanticVersion.parse(value);
            return null;
        } catch (VersionParsingException exception) {
            return exception;
        }
    }

    private static void assertPredicate(String expression, List<String> matches,
            List<String> rejects) throws Exception {
        VersionPredicate predicate = VersionPredicate.parse(expression);
        for (String version : matches) {
            assertThat(predicate.test(Version.parse(version)))
                    .as("%s should match %s", version, expression).isTrue();
        }
        for (String version : rejects) {
            assertThat(predicate.test(Version.parse(version)))
                    .as("%s should not match %s", version, expression).isFalse();
        }
    }
}
