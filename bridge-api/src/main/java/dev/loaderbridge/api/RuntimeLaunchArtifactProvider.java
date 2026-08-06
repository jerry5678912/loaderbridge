package dev.loaderbridge.api;

import java.io.IOException;
import java.nio.file.Path;

/** ServiceLoader extension point for agents and other pre-bootstrap runtime files. */
public interface RuntimeLaunchArtifactProvider {
    RuntimeLaunchArtifact descriptor();

    Path artifact() throws IOException;
}
