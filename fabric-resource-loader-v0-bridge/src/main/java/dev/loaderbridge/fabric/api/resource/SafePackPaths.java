package dev.loaderbridge.fabric.api.resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class SafePackPaths {
    private SafePackPaths() { }

    static Path containedDirectory(Path boundary, Path candidate) {
        return contained(boundary, candidate, true);
    }

    static Path containedRegularFile(Path boundary, Path candidate) {
        return contained(boundary, candidate, false);
    }

    private static Path contained(Path boundary, Path candidate, boolean directory) {
        try {
            Path realBoundary = boundary.toRealPath();
            Path realCandidate = candidate.toRealPath();
            if (!realCandidate.startsWith(realBoundary)) return null;
            if (directory ? !Files.isDirectory(realCandidate) : !Files.isRegularFile(realCandidate)) {
                return null;
            }
            return realCandidate;
        } catch (IOException | SecurityException exception) {
            return null;
        }
    }
}
