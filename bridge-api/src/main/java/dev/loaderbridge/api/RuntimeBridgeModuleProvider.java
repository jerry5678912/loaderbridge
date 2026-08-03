package dev.loaderbridge.api;

import java.io.IOException;
import java.nio.file.Path;

/** ServiceLoader extension point for automatically installed compatibility modules. */
public interface RuntimeBridgeModuleProvider {
    RuntimeBridgeModule descriptor();

    /** Returns the immutable local JAR to install in the prepared Forge instance. */
    Path artifact() throws IOException;
}
