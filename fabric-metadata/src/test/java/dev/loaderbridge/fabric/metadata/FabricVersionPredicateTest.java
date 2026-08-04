package dev.loaderbridge.fabric.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

final class FabricVersionPredicateTest {
    @Test
    void evaluatesCommonFabricVersionPredicateForms() {
        assertThat(FabricVersionPredicate.anyMatches(List.of("*"), "garbage-version")).isTrue();
        assertThat(FabricVersionPredicate.anyMatches(List.of(">=0.16.0"), "0.16.14")).isTrue();
        assertThat(FabricVersionPredicate.anyMatches(List.of("~1.21.1"), "1.21.1")).isTrue();
        assertThat(FabricVersionPredicate.anyMatches(List.of("~1.21.1"), "1.22.0")).isFalse();
        assertThat(FabricVersionPredicate.anyMatches(List.of("1.21.x"), "1.21.4")).isTrue();
        assertThat(FabricVersionPredicate.anyMatches(List.of("1.21.x"), "1.22.0")).isFalse();
        assertThat(FabricVersionPredicate.anyMatches(List.of("<2.0.0", ">=3.0.0"), "3.1.0")).isTrue();
        assertThat(FabricVersionPredicate.matches("=1.0.0", "1.0.0+mc1.21.1")).isTrue();
        assertThat(FabricVersionPredicate.compare("1.0.0+build.2", "1.0.0+build.1")).isZero();
    }
}
