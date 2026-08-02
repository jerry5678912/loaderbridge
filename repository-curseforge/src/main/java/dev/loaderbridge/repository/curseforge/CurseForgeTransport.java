package dev.loaderbridge.repository.curseforge;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

interface CurseForgeTransport {
    byte[] read(URI uri, long maximumBytes) throws IOException, InterruptedException;

    void download(URI uri, Path destination, long maximumBytes) throws IOException, InterruptedException;
}
