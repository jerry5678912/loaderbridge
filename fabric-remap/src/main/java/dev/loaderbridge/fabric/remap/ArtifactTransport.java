package dev.loaderbridge.fabric.remap;

import java.io.IOException;
import java.net.URI;

@FunctionalInterface
public interface ArtifactTransport {
    byte[] read(URI uri, long maximumBytes) throws IOException, InterruptedException;
}
