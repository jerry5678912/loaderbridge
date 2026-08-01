package dev.loaderbridge.fabric.remap;

import java.util.Set;

public record ReferenceInventory(Set<String> fabricApiClasses, Set<String> loaderApiClasses,
        Set<String> minecraftClasses,
        Set<String> reflectionSensitiveStrings, Set<String> nativeLibraries) {
    public ReferenceInventory {
        fabricApiClasses = Set.copyOf(fabricApiClasses);
        loaderApiClasses = Set.copyOf(loaderApiClasses);
        minecraftClasses = Set.copyOf(minecraftClasses);
        reflectionSensitiveStrings = Set.copyOf(reflectionSensitiveStrings);
        nativeLibraries = Set.copyOf(nativeLibraries);
    }
}
