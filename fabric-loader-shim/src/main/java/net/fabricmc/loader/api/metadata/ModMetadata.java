package net.fabricmc.loader.api.metadata;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.loader.api.Version;

public interface ModMetadata {
    default String getType() { return "fabric"; }

    String getId();

    Collection<String> getProvides();

    Version getVersion();

    default ModEnvironment getEnvironment() { return ModEnvironment.UNIVERSAL; }

    default Collection<ModDependency> getDependencies() { return java.util.List.of(); }

    default Collection<ModDependency> getDepends() { return dependenciesOf(ModDependency.Kind.DEPENDS); }
    default Collection<ModDependency> getRecommends() { return dependenciesOf(ModDependency.Kind.RECOMMENDS); }
    default Collection<ModDependency> getSuggests() { return dependenciesOf(ModDependency.Kind.SUGGESTS); }
    default Collection<ModDependency> getConflicts() { return dependenciesOf(ModDependency.Kind.CONFLICTS); }
    default Collection<ModDependency> getBreaks() { return dependenciesOf(ModDependency.Kind.BREAKS); }

    String getName();

    default String getDescription() { return ""; }
    default Collection<Person> getAuthors() { return java.util.List.of(); }
    default Collection<Person> getContributors() { return java.util.List.of(); }
    default ContactInformation getContact() { return ContactInformation.EMPTY; }
    default Collection<String> getLicense() { return java.util.List.of(); }
    default Optional<String> getIconPath(int size) { return Optional.empty(); }
    default boolean containsCustomValue(String key) { return getCustomValues().containsKey(key); }
    default CustomValue getCustomValue(String key) { return getCustomValues().get(key); }
    default Map<String, CustomValue> getCustomValues() { return Map.of(); }
    default boolean containsCustomElement(String key) { return containsCustomValue(key); }

    private Collection<ModDependency> dependenciesOf(ModDependency.Kind kind) {
        return getDependencies().stream().filter(dependency -> dependency.getKind() == kind).toList();
    }
}
