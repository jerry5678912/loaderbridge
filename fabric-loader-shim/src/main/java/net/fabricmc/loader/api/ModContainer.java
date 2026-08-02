package net.fabricmc.loader.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.ModOrigin;

public interface ModContainer {
    ModMetadata getMetadata();

    List<Path> getRootPaths();

    default Optional<Path> findPath(String file) {
        return getRootPaths().stream()
                .map(root -> root.resolve(file.replace("/", root.getFileSystem().getSeparator())))
                .filter(Files::exists)
                .findFirst();
    }

    ModOrigin getOrigin();

    Optional<ModContainer> getContainingMod();

    Collection<ModContainer> getContainedMods();

    @Deprecated
    default Path getRoot() { return getRootPath(); }

    @Deprecated
    Path getRootPath();

    @Deprecated
    Path getPath(String file);
}
