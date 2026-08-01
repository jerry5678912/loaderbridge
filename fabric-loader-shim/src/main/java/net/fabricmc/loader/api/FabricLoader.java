package net.fabricmc.loader.api;

import dev.loaderbridge.fabric.runtime.BridgeFabricLoader;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;

/** Common Fabric Loader API surface. Unsupported methods are added as compatibility grows. */
public interface FabricLoader {
    static FabricLoader getInstance() {
        return BridgeFabricLoader.getInstance();
    }

    <T> List<T> getEntrypoints(String key, Class<T> type);

    default <T> void invokeEntrypoints(String key, Class<T> type, Consumer<? super T> invoker) {
        getEntrypoints(key, type).forEach(invoker);
    }

    ObjectShare getObjectShare();

    MappingResolver getMappingResolver();

    Optional<ModContainer> getModContainer(String id);

    Collection<ModContainer> getAllMods();

    boolean isModLoaded(String id);

    EnvType getEnvironmentType();

    Path getGameDir();

    Path getConfigDir();

    boolean isDevelopmentEnvironment();
}
