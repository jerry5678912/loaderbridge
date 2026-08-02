package dev.loaderbridge.fabric.runtime;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Locale;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.EntrypointException;
import net.fabricmc.loader.api.MappingResolver;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.ObjectShare;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

public final class BridgeFabricLoader implements FabricLoader {
    private static final BridgeFabricLoader INSTANCE = new BridgeFabricLoader();
    private static final Set<String> SENSITIVE_ARGUMENTS = Set.of(
            "accesstoken", "clientid", "profileproperties", "proxypass", "proxyuser",
            "username", "userproperties", "uuid", "xuid");
    private final Map<String, ModContainer> mods = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<String, List<RegisteredEntrypoint>> entrypoints = Collections.synchronizedMap(new LinkedHashMap<>());
    private final BridgeObjectShare objectShare = new BridgeObjectShare();
    private final BridgeMappingResolver mappingResolver = new BridgeMappingResolver();
    private volatile EnvType environment = EnvType.CLIENT;
    private volatile Path gameDirectory = Path.of(".").toAbsolutePath().normalize();
    private volatile boolean developmentEnvironment;
    private volatile Object gameInstance;
    private volatile String[] launchArguments = new String[0];

    private BridgeFabricLoader() {}

    public static BridgeFabricLoader getInstance() {
        return INSTANCE;
    }

    public void configure(EnvType type, Path gameDir) {
        configure(type, gameDir, false, null, new String[0]);
    }

    public void configure(EnvType type, Path gameDir, boolean development, Object game,
            String[] arguments) {
        environment = type;
        gameDirectory = gameDir.toAbsolutePath().normalize();
        developmentEnvironment = development;
        gameInstance = game;
        launchArguments = arguments.clone();
    }

    public void registerMod(ModContainer container) {
        mods.put(container.getMetadata().getId(), container);
        container.getMetadata().getProvides().forEach(alias -> mods.put(alias, container));
    }

    public void installMappings(Path path) throws java.io.IOException {
        mappingResolver.install(path);
    }

    public void registerEntrypoint(String key, Object entrypoint) {
        ModContainer provider;
        synchronized (mods) {
            provider = mods.values().stream().reduce((first, second) -> second)
                    .orElseThrow(() -> new IllegalStateException("entrypoint provider is not registered"));
        }
        registerEntrypoint(key, provider, entrypoint.getClass().getName(), entrypoint);
    }

    public void registerEntrypoint(String key, ModContainer provider, String definition, Object entrypoint) {
        entrypoints.computeIfAbsent(key, ignored -> Collections.synchronizedList(new ArrayList<>()))
                .add(new RegisteredEntrypoint(provider, definition, entrypoint));
    }

    @Override
    public <T> List<T> getEntrypoints(String key, Class<T> type) {
        return getEntrypointContainers(key, type).stream().map(EntrypointContainer::getEntrypoint).toList();
    }

    @Override
    @SuppressWarnings("deprecation")
    public <T> List<EntrypointContainer<T>> getEntrypointContainers(String key, Class<T> type) {
        List<EntrypointContainer<T>> containers = new ArrayList<>();
        for (RegisteredEntrypoint entrypoint : entrypoints.getOrDefault(key, List.of())) {
            try {
                containers.add(entrypoint.container(type));
            } catch (Throwable cause) {
                throw new EntrypointException(key,
                        entrypoint.provider().getMetadata().getId(), cause);
            }
        }
        return List.copyOf(containers);
    }

    @Override
    public <T> void invokeEntrypoints(String key, Class<T> type, java.util.function.Consumer<? super T> invoker) {
        RuntimeException failure = null;
        for (EntrypointContainer<T> container : getEntrypointContainers(key, type)) {
            try {
                invoker.accept(container.getEntrypoint());
            } catch (Throwable cause) {
                if (failure == null) {
                    failure = new RuntimeException("Could not execute entrypoint stage '" + key
                            + "' due to errors, provided by '"
                            + container.getProvider().getMetadata().getId() + "' at '"
                            + container.getDefinition() + "'!", cause);
                } else {
                    failure.addSuppressed(cause);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    @Override
    public ObjectShare getObjectShare() {
        return objectShare;
    }

    @Override
    public MappingResolver getMappingResolver() {
        return mappingResolver;
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
    @Override @Deprecated public Object getGameInstance() { return gameInstance; }
    @Override public Path getGameDir() { return gameDirectory; }
    @Override @Deprecated public File getGameDirectory() { return gameDirectory.toFile(); }
    @Override public Path getConfigDir() { return gameDirectory.resolve("config"); }
    @Override @Deprecated public File getConfigDirectory() { return getConfigDir().toFile(); }
    @Override public boolean isDevelopmentEnvironment() { return developmentEnvironment; }

    @Override
    public String[] getLaunchArguments(boolean sanitize) {
        // Sensitive keys match Fabric Loader 0.16.14's MinecraftGameProvider contract.
        // Source: https://github.com/FabricMC/fabric-loader/blob/0.16.14/minecraft/src/main/java/net/fabricmc/loader/impl/game/minecraft/MinecraftGameProvider.java
        String[] arguments = launchArguments.clone();
        if (!sanitize) {
            return arguments;
        }
        List<String> safe = new ArrayList<>(arguments.length);
        for (int index = 0; index < arguments.length; index++) {
            String argument = arguments[index];
            if (index + 1 < arguments.length && argument.startsWith("--")
                    && SENSITIVE_ARGUMENTS.contains(argument.substring(2).toLowerCase(Locale.ENGLISH))) {
                index++;
            } else {
                safe.add(argument);
            }
        }
        return safe.toArray(String[]::new);
    }

    void resetForTests() {
        mods.clear();
        entrypoints.clear();
        objectShare.clear();
        configure(EnvType.CLIENT, Path.of("."), false, null, new String[0]);
    }

    private record RegisteredEntrypoint(ModContainer provider, String definition, Object value) {
        <T> EntrypointContainer<T> container(Class<T> type) {
            T typed = type.cast(value);
            return new EntrypointContainer<>() {
                @Override public T getEntrypoint() { return typed; }
                @Override public ModContainer getProvider() { return provider; }
                @Override public String getDefinition() { return definition; }
            };
        }
    }

}
