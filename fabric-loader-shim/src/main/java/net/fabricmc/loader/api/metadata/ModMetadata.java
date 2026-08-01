package net.fabricmc.loader.api.metadata;

import java.util.Collection;

public interface ModMetadata {
    String getId();

    Collection<String> getProvides();

    Version getVersion();

    String getName();
}
