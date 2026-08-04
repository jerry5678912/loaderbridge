package dev.loaderbridge.fabric.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class FabricCandidateSelectorTest {
    @Test
    void selectsTheHighestCandidateCompatibleWithTheMinecraftVersion() {
        FabricModMetadata incompatible = metadata("library", "2.0.0",
                Map.of("minecraft", List.of(">=1.22")), Map.of());
        FabricModMetadata compatible = metadata("library", "1.5.0",
                Map.of("minecraft", List.of("1.21.x")), Map.of());

        var result = FabricCandidateSelector.select(List.of(
                candidate(incompatible, false), candidate(compatible, false)),
                Map.of("minecraft", "1.21.1"));

        assertThat(result.solved()).isTrue();
        assertThat(result.selected()).extracting(FabricModMetadata::version)
                .containsExactly("1.5.0");
    }

    @Test
    void backtracksAcrossMutuallyConstrainedCandidateGroups() {
        FabricModMetadata a2 = metadata("a", "2", Map.of("b", List.of("2")), Map.of());
        FabricModMetadata a1 = metadata("a", "1", Map.of("b", List.of("1")), Map.of());
        FabricModMetadata b2 = metadata("b", "2", Map.of("a", List.of("1")), Map.of());
        FabricModMetadata b1 = metadata("b", "1", Map.of("a", List.of("1")), Map.of());

        var result = FabricCandidateSelector.select(List.of(
                candidate(a2, false), candidate(a1, false),
                candidate(b2, false), candidate(b1, false)), Map.of());

        assertThat(result.solved()).isTrue();
        assertThat(result.selected()).extracting(FabricModMetadata::id,
                        FabricModMetadata::version)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("a", "1"),
                        org.assertj.core.groups.Tuple.tuple("b", "1"));
    }

    @Test
    void reportsUnsatisfiableAlternativesAndDuplicateRoots() {
        FabricModMetadata root = metadata("root", "1", Map.of("a", List.of("*")), Map.of());
        FabricModMetadata a2 = metadata("a", "2", Map.of("b", List.of("2")), Map.of());
        FabricModMetadata a1 = metadata("a", "1", Map.of("b", List.of("1")), Map.of());
        FabricModMetadata b2 = metadata("b", "2", Map.of("a", List.of("1")), Map.of());
        FabricModMetadata b1 = metadata("b", "1", Map.of("a", List.of("2")), Map.of());

        var unsatisfiable = FabricCandidateSelector.select(List.of(
                candidate(root, true),
                candidate(a2, false), candidate(a1, false),
                candidate(b2, false), candidate(b1, false)), Map.of());
        var duplicateRoots = FabricCandidateSelector.select(List.of(
                candidate(a1, true), candidate(a2, true)), Map.of());

        assertThat(unsatisfiable.status())
                .isEqualTo(FabricCandidateSelector.Status.UNSATISFIABLE);
        assertThat(duplicateRoots.status())
                .isEqualTo(FabricCandidateSelector.Status.DUPLICATE_ROOTS);
    }

    @Test
    void loadsOnlyChildrenReachableFromTheSelectedParentVariant() {
        FabricModMetadata root = metadata("root", "1", Map.of("parent", List.of("1")), Map.of());
        FabricModMetadata parent2 = metadata("parent", "2", Map.of(), Map.of());
        FabricModMetadata parent1 = metadata("parent", "1", Map.of(), Map.of());
        FabricModMetadata child2 = metadata("child_from_two", "1", Map.of(), Map.of());
        FabricModMetadata child1 = metadata("child_from_one", "1", Map.of(), Map.of());
        var rootCandidate = candidate(root, true);
        var parentTwoCandidate = candidate(parent2, false, "parent-2", Set.of(), 1);
        var parentOneCandidate = candidate(parent1, false, "parent-1", Set.of(), 1);

        var result = FabricCandidateSelector.select(List.of(rootCandidate,
                parentTwoCandidate, parentOneCandidate,
                candidate(child2, false, "child-2", Set.of("parent-2"), 2),
                candidate(child1, false, "child-1", Set.of("parent-1"), 2)), Map.of());

        assertThat(result.solved()).isTrue();
        assertThat(result.selected()).extracting(FabricModMetadata::id)
                .containsExactly("root", "parent", "child_from_one");
    }

    @Test
    void omitsAnOptionalNestedCandidateWhenItsHardConstraintsConflict() {
        FabricModMetadata root = metadata("root", "1", Map.of(), Map.of());
        FabricModMetadata incompatible = metadata(
                "optional", "1", Map.of(), Map.of("root", List.of("*")));

        var result = FabricCandidateSelector.select(List.of(
                candidate(root, true), candidate(incompatible, false)), Map.of());

        assertThat(result.solved()).isTrue();
        assertThat(result.selected()).containsExactly(root);
    }

    @Test
    void omitsAnOptionalNestedCandidateWithAMissingDependency() {
        FabricModMetadata root = metadata("root", "1", Map.of(), Map.of());
        FabricModMetadata incomplete = metadata(
                "optional", "1", Map.of("missing_library", List.of("*")), Map.of());

        var result = FabricCandidateSelector.select(List.of(
                candidate(root, true), candidate(incomplete, false)), Map.of());

        assertThat(result.solved()).isTrue();
        assertThat(result.selected()).containsExactly(root);
    }

    private static FabricCandidateSelector.Candidate<FabricModMetadata> candidate(
            FabricModMetadata metadata, boolean root) {
        return new FabricCandidateSelector.Candidate<>(metadata, metadata, root);
    }

    private static FabricCandidateSelector.Candidate<FabricModMetadata> candidate(
            FabricModMetadata metadata, boolean root, String key, Set<String> parents, int depth) {
        return new FabricCandidateSelector.Candidate<>(
                metadata, metadata, root, key, parents, depth);
    }

    private static FabricModMetadata metadata(String id, String version,
            Map<String, List<String>> depends, Map<String, List<String>> breaks) {
        return new FabricModMetadata(1, id, version, id, "*", Map.of(),
                new FabricDependencies(depends, Map.of(), Map.of(), breaks, Map.of()),
                List.of(), List.of(), Optional.empty(), List.of(), Map.of());
    }
}
