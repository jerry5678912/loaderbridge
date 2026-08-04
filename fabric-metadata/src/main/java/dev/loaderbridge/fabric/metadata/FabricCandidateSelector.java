package dev.loaderbridge.fabric.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Deterministically selects one dependency-consistent candidate for every Fabric mod ID. */
public final class FabricCandidateSelector {
    private static final long MAX_COMBINATIONS = 1_000_000;

    private FabricCandidateSelector() {}

    public static <T> Result<T> select(List<Candidate<T>> candidates,
            Map<String, String> availableVersions) {
        Map<String, List<Candidate<T>>> groups = new TreeMap<>();
        candidates.forEach(candidate -> groups.computeIfAbsent(candidate.metadata().id(),
                ignored -> new ArrayList<>()).add(candidate));
        for (Map.Entry<String, List<Candidate<T>>> entry : groups.entrySet()) {
            List<Candidate<T>> roots = entry.getValue().stream().filter(Candidate::root).toList();
            if (roots.size() > 1) {
                return fallback(candidates, groups, Status.DUPLICATE_ROOTS,
                        "Duplicate root Fabric mods claim ID '" + entry.getKey() + "'");
            }
        }

        List<Candidate<T>> fixed = new ArrayList<>();
        List<List<Candidate<T>>> variables = new ArrayList<>();
        for (List<Candidate<T>> group : groups.values()) {
            Candidate<T> root = group.stream().filter(Candidate::root).findFirst().orElse(null);
            if (root != null) {
                fixed.add(root);
            } else if (group.size() == 1) {
                fixed.add(group.getFirst());
            } else {
                List<Candidate<T>> ordered = new ArrayList<>(group);
                ordered.sort(Comparator.comparing((Candidate<T> candidate) ->
                        candidate.metadata().version(), FabricVersionPredicate::compare).reversed());
                variables.add(List.copyOf(ordered));
            }
        }

        Set<String> knownIdentities = new LinkedHashSet<>(availableVersions.keySet());
        candidates.forEach(candidate -> {
            knownIdentities.add(candidate.metadata().id());
            knownIdentities.addAll(candidate.metadata().provides());
        });
        SearchState<T> state = new SearchState<>(availableVersions, knownIdentities);
        List<Candidate<T>> selected = new ArrayList<>(fixed);
        if (variables.isEmpty()) {
            return resultInInputOrder(candidates, selected, Status.SOLVED, "");
        }
        if (search(variables, 0, selected, state)) {
            return resultInInputOrder(candidates, state.solution, Status.SOLVED, "");
        }
        Status status = state.explored >= MAX_COMBINATIONS
                ? Status.BUDGET_EXCEEDED : Status.UNSATISFIABLE;
        String detail = status == Status.BUDGET_EXCEEDED
                ? "Fabric candidate search exceeded " + MAX_COMBINATIONS + " combinations"
                : "No dependency-consistent Fabric candidate combination exists";
        return fallback(candidates, groups, status, detail);
    }

    private static <T> boolean search(List<List<Candidate<T>>> variables, int index,
            List<Candidate<T>> selected, SearchState<T> state) {
        if (state.explored >= MAX_COMBINATIONS) return false;
        if (index == variables.size()) {
            state.explored++;
            if (!valid(selected, state.availableVersions, state.knownIdentities)) return false;
            state.solution = List.copyOf(selected);
            return true;
        }
        for (Candidate<T> candidate : variables.get(index)) {
            selected.add(candidate);
            if (search(variables, index + 1, selected, state)) return true;
            selected.removeLast();
        }
        return false;
    }

    private static <T> boolean valid(List<Candidate<T>> selected,
            Map<String, String> availableVersions, Set<String> knownIdentities) {
        Map<String, String> claims = new LinkedHashMap<>(availableVersions);
        Set<String> selectedClaims = new LinkedHashSet<>();
        for (Candidate<T> candidate : selected) {
            FabricModMetadata metadata = candidate.metadata();
            List<String> identities = new ArrayList<>(metadata.provides());
            identities.add(metadata.id());
            for (String identity : identities) {
                if (!selectedClaims.add(identity)) return false;
                claims.put(identity, metadata.version());
            }
        }
        for (Candidate<T> candidate : selected) {
            FabricDependencies dependencies = candidate.metadata().dependencies();
            for (Map.Entry<String, List<String>> dependency : dependencies.depends().entrySet()) {
                if (!knownIdentities.contains(dependency.getKey())) continue;
                String version = claims.get(dependency.getKey());
                if (version == null || !FabricVersionPredicate.anyMatches(
                        dependency.getValue(), version)) return false;
            }
            for (Map.Entry<String, List<String>> broken : dependencies.breaks().entrySet()) {
                String version = claims.get(broken.getKey());
                if (version != null && FabricVersionPredicate.anyMatches(
                        broken.getValue(), version)) return false;
            }
        }
        return true;
    }

    private static <T> Result<T> fallback(List<Candidate<T>> candidates,
            Map<String, List<Candidate<T>>> groups, Status status, String detail) {
        Set<Candidate<T>> selected = Collections.newSetFromMap(new IdentityHashMap<>());
        for (List<Candidate<T>> group : groups.values()) {
            Candidate<T> root = group.stream().filter(Candidate::root).findFirst().orElse(null);
            if (root != null) {
                selected.add(root);
                continue;
            }
            Candidate<T> best = group.getFirst();
            for (int index = 1; index < group.size(); index++) {
                Candidate<T> candidate = group.get(index);
                if (FabricVersionPredicate.compare(candidate.metadata().version(),
                        best.metadata().version()) > 0) best = candidate;
            }
            selected.add(best);
        }
        return resultInInputOrder(candidates, selected, status, detail);
    }

    private static <T> Result<T> resultInInputOrder(List<Candidate<T>> candidates,
            java.util.Collection<Candidate<T>> selected, Status status, String detail) {
        Set<Candidate<T>> selectedByIdentity = Collections.newSetFromMap(new IdentityHashMap<>());
        selectedByIdentity.addAll(selected);
        return new Result<>(candidates.stream().filter(selectedByIdentity::contains)
                .map(Candidate::value).toList(), status, detail);
    }

    public record Candidate<T>(T value, FabricModMetadata metadata, boolean root) {
        public Candidate {
            java.util.Objects.requireNonNull(value, "value");
            java.util.Objects.requireNonNull(metadata, "metadata");
        }
    }

    public record Result<T>(List<T> selected, Status status, String detail) {
        public Result {
            selected = List.copyOf(selected);
            java.util.Objects.requireNonNull(status, "status");
            java.util.Objects.requireNonNull(detail, "detail");
        }

        public boolean solved() {
            return status == Status.SOLVED;
        }
    }

    public enum Status {
        SOLVED,
        DUPLICATE_ROOTS,
        UNSATISFIABLE,
        BUDGET_EXCEEDED
    }

    private static final class SearchState<T> {
        private final Map<String, String> availableVersions;
        private final Set<String> knownIdentities;
        private long explored;
        private List<Candidate<T>> solution = List.of();

        private SearchState(Map<String, String> availableVersions, Set<String> knownIdentities) {
            this.availableVersions = Map.copyOf(availableVersions);
            this.knownIdentities = Set.copyOf(knownIdentities);
        }
    }
}
