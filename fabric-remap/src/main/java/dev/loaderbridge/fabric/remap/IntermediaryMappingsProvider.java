package dev.loaderbridge.fabric.remap;

import java.io.IOException;
import java.nio.file.Path;

@FunctionalInterface
public interface IntermediaryMappingsProvider {
    Path resolve(String minecraftVersion, Path cacheDirectory) throws IOException;
}
