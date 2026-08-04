package dev.loaderbridge.fabric.remap;

import java.util.Set;

public record ReferenceInventory(Set<String> fabricApiClasses, Set<String> loaderApiClasses,
        Set<String> mixinExtrasClasses,
        Set<String> minecraftClasses,
        Set<String> reflectionSensitiveStrings, Set<String> nativeLibraries,
        Set<String> mixinSemanticFeatures, Set<String> structuredResourceFeatures) {
    public ReferenceInventory {
        fabricApiClasses = Set.copyOf(fabricApiClasses);
        loaderApiClasses = Set.copyOf(loaderApiClasses);
        mixinExtrasClasses = Set.copyOf(mixinExtrasClasses);
        minecraftClasses = Set.copyOf(minecraftClasses);
        reflectionSensitiveStrings = Set.copyOf(reflectionSensitiveStrings);
        nativeLibraries = Set.copyOf(nativeLibraries);
        mixinSemanticFeatures = Set.copyOf(mixinSemanticFeatures);
        structuredResourceFeatures = Set.copyOf(structuredResourceFeatures);
    }
}
