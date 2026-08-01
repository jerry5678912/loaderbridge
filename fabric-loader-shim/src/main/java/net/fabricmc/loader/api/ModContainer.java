package net.fabricmc.loader.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import net.fabricmc.loader.api.metadata.ModMetadata;

public interface ModContainer {
    ModMetadata getMetadata();

    List<Path> getRootPaths();

    default Optional<Path> findPath(String file) {
        return getRootPaths().stream().map(root -> root.resolve(file)).filter(Files::exists).findFirst();
    }
}
