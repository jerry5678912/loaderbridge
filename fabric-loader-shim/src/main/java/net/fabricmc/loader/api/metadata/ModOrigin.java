package net.fabricmc.loader.api.metadata;

import java.nio.file.Path;
import java.util.List;

public interface ModOrigin {
    Kind getKind();
    List<Path> getPaths();
    String getParentModId();
    String getParentSubLocation();

    enum Kind {
        PATH,
        NESTED,
        UNKNOWN
    }
}
