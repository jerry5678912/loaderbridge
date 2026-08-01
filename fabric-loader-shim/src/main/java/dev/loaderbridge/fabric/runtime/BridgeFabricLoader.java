package dev.loaderbridge.fabric.runtime;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.ObjectShare;

public final class BridgeFabricLoader implements FabricLoader {
    private static final BridgeFabricLoader INSTANCE = new BridgeFabricLoader();
    private final Map<String, ModContainer> mods = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<String, List<Object>> entrypoints = new ConcurrentHashMap<>();
    private final Map<String, Object> sharedObjects = new ConcurrentHashMap<>();
    private volatile EnvType environment = EnvType.CLIENT;
    private volatile Path gameDirectory = Path.of(".").toAbsolutePath().normalize();

    private BridgeFabricLoader() {}

    public static BridgeFabricLoader getInstance() {
        return INSTANCE;
    }

    public void configure(EnvType type, Path gameDir) {
        environment = type;
        gameDirectory = gameDir.toAbsolutePath().normalize();
    }

    public void registerMod(ModContainer container) {
        mods.put(container.getMetadata().getId(), container);
        container.getMetadata().getProvides().forEach(alias -> mods.put(alias, container));
    }

    public void registerEntrypoint(String key, Object entrypoint) {
        entrypoints.computeIfAbsent(key, ignored -> Collections.synchronizedList(new ArrayList<>()))
                .add(entrypoint);
    }

    @Override
    public <T> List<T> getEntrypoints(String key, Class<T> type) {
        return entrypoints.getOrDefault(key, List.of()).stream().map(type::cast).toList();
    }

    @Override
    public ObjectShare getObjectShare() {
        return new ObjectShare() {
            @Override public Object get(String key) { return sharedObjects.get(key); }
            @Override public Object put(String key, Object value) { return sharedObjects.put(key, value); }
            @Override public Object putIfAbsent(String key, Object value) {
                return sharedObjects.putIfAbsent(key, value);
            }
            @Override public Object remove(String key) { return sharedObjects.remove(key); }
        };
    }

    @Override
    public MappingResolver getMappingResolver() {
        return IdentityMappingResolver.INSTANCE;
    }

    @Override
    public Optional<ModContainer> getModContainer(String id) {
        return Optional.ofNullable(mods.get(id));
    }

    @Override
    public Collection<ModContainer> getAllMods() {
        synchronized (mods) {
            return List.copyOf(new java.util.LinkedHashSet<>(mods.values()));
        }
    }

    @Override public boolean isModLoaded(String id) { return mods.containsKey(id); }
    @Override public EnvType getEnvironmentType() { return environment; }
    @Override public Path getGameDir() { return gameDirectory; }
    @Override public Path getConfigDir() { return gameDirectory.resolve("config"); }
    @Override public boolean isDevelopmentEnvironment() { return false; }

    private enum IdentityMappingResolver implements MappingResolver {
        INSTANCE;

        @Override public Collection<String> getNamespaces() { return List.of("official"); }
        @Override public String getCurrentRuntimeNamespace() { return "official"; }
        @Override public String mapClassName(String namespace, String className) { return className; }
        @Override public String unmapClassName(String targetNamespace, String className) { return className; }
        @Override public String mapFieldName(String namespace, String owner, String name, String descriptor) {
            return name;
        }
        @Override public String mapMethodName(String namespace, String owner, String name, String descriptor) {
            return name;
        }
    }
}
