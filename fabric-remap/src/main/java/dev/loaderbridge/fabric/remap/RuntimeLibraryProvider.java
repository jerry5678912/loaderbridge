package dev.loaderbridge.fabric.remap;

import java.io.IOException;
import java.nio.file.Path;

@FunctionalInterface
interface RuntimeLibraryProvider {
    ResolvedRuntimeLibrary resolve(Path cacheDirectory, boolean refresh) throws IOException;
}
