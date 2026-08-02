package dev.loaderbridge.fabric.remap;

import java.io.IOException;
import java.nio.file.Path;

@FunctionalInterface
public interface MinecraftArtifactsProvider {
    ResolvedMinecraftArtifacts resolve(String version, Path cacheDirectory, boolean refresh) throws IOException;
}
