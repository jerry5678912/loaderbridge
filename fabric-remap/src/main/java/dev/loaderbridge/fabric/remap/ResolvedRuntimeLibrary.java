package dev.loaderbridge.fabric.remap;

import java.net.URI;
import java.nio.file.Path;

record ResolvedRuntimeLibrary(String id, String version, URI url, String sha256, Path path) {}
