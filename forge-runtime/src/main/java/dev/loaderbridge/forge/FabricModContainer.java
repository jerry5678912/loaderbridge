package dev.loaderbridge.forge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.loaderbridge.fabric.runtime.BridgeFabricLoader;
import dev.loaderbridge.fabric.runtime.BridgeModContainer;
import dev.loaderbridge.fabric.runtime.BridgeKotlinLanguageAdapter;
import dev.loaderbridge.fabric.metadata.FabricMetadataParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.event.IModBusEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.language.ModFileScanData;

public final class FabricModContainer extends ModContainer {
    private final List<Object> modInstances = new ArrayList<>();
    private final BridgeModContainer bridgeModContainer;
    private final ClassLoader gameClassLoader;
    private final boolean active;

    public FabricModContainer(IModInfo info, ModFileScanData scanData, ModuleLayer gameLayer) {
        super(info);
        gameClassLoader = gameLayer.findLoader("minecraft");
        this.contextExtension = () -> null;
        Path metadataPath = info.getOwningFile().getFile().findResource("fabric.mod.json");
        Path root = metadataPath.getParent();
        String minecraftVersion;
        List<FabricEntrypointDefinitions.Declaration> entrypointDefinitions;
        try {
            JsonObject bridgeMetadata = JsonParser.parseString(Files.readString(
                    root.resolve("META-INF/loaderbridge.json"))).getAsJsonObject();
            minecraftVersion = bridgeMetadata.get("minecraftVersion").getAsString();
            String parentModId = bridgeMetadata.has("parentModId")
                    ? bridgeMetadata.get("parentModId").getAsString() : null;
            String parentSubLocation = bridgeMetadata.has("parentSubLocation")
                    ? bridgeMetadata.get("parentSubLocation").getAsString() : null;
            bridgeModContainer = BridgeModContainer.create(
                    new FabricMetadataParser().parse(Files.readAllBytes(metadataPath)), root,
                    parentModId, parentSubLocation);
            entrypointDefinitions = FabricEntrypointDefinitions.parse(
                    JsonParser.parseString(Files.readString(metadataPath)).getAsJsonObject());
        } catch (IOException exception) {
            throw new IllegalStateException("LB-META-010: failed to register runtime metadata for "
                    + info.getModId(), exception);
        }
        net.fabricmc.api.EnvType environment = FMLEnvironment.dist.isClient()
                ? net.fabricmc.api.EnvType.CLIENT : net.fabricmc.api.EnvType.SERVER;
        List<Path> minecraftRoots = FMLLoader.getLaunchHandler().getMinecraftPaths();
        if (minecraftRoots.isEmpty()) {
            throw new IllegalStateException(
                    "LB-LOADER-ROOT-001: Forge reported no Minecraft input paths");
        }
        BridgeFabricLoader.getInstance().configureHost(
                environment, FMLPaths.GAMEDIR.get(), minecraftVersion,
                !FMLEnvironment.production, minecraftRoots);
        if (environment == net.fabricmc.api.EnvType.SERVER) {
            FabricServerGameInstanceRegistration.install(gameClassLoader);
        }
        try {
            BridgeFabricLoader.getInstance().installMappings(
                    root.resolve("META-INF/loaderbridge/mappings.tiny"));
        } catch (IOException exception) {
            throw new IllegalStateException("LB-MAP-003: failed to install runtime mappings for "
                    + info.getModId(), exception);
        }
        active = bridgeModContainer.getMetadata().getEnvironment().matches(environment);
        if (!active) {
            return;
        }
        BridgeFabricLoader.getInstance().registerMod(bridgeModContainer);
        registerEntrypointDefinitions(entrypointDefinitions);
        FabricRegistrationLifecycle.registerPreLaunchEntrypoints(
                bridgeModContainer.getMetadata().getId(),
                () -> invokeEntrypoints("preLaunch", PreLaunchEntrypoint.class,
                        entrypoint -> entrypoint.onPreLaunch()));
        FabricRegistrationLifecycle.registerMainEntrypoints(
                bridgeModContainer.getMetadata().getId(),
                () -> invokeEntrypoints("main", ModInitializer.class,
                        initializer -> initializer.onInitialize()));
        if (FMLEnvironment.dist.isClient()) {
            FabricRegistrationLifecycle.registerClientEntrypoints(
                    bridgeModContainer.getMetadata().getId(),
                    () -> invokeEntrypoints("client", ClientModInitializer.class,
                            initializer -> initializer.onInitializeClient()));
        } else {
            FabricRegistrationLifecycle.registerServerEntrypoints(
                    bridgeModContainer.getMetadata().getId(),
                    () -> invokeEntrypoints("server", DedicatedServerModInitializer.class,
                            initializer -> initializer.onInitializeServer()));
        }
    }

    @SuppressWarnings("try")
    private void registerEntrypointDefinitions(
            List<FabricEntrypointDefinitions.Declaration> declarations) {
        for (FabricEntrypointDefinitions.Declaration declaration : declarations) {
            LanguageAdapter adapter = languageAdapter(declaration.adapter());
            BridgeFabricLoader.getInstance().registerEntrypointDefinition(
                    declaration.key(), bridgeModContainer, declaration.value(), type -> {
                        Object instance;
                        try (ContextClassLoaderScope ignored =
                                ContextClassLoaderScope.open(gameClassLoader)) {
                            instance = adapter.create(
                                    bridgeModContainer, declaration.value(), type);
                        }
                        rememberModInstance(instance);
                        return instance;
                    });
        }
    }

    private LanguageAdapter languageAdapter(String adapter) {
        return switch (adapter) {
            case "default" -> LanguageAdapter.getDefault();
            case "kotlin" -> BridgeKotlinLanguageAdapter.INSTANCE;
            default -> throw new IllegalStateException(
                    "LB-ENTRY-004: unsupported language adapter '" + adapter + "' for " + modId);
        };
    }

    private <T> void invokeEntrypoints(String key, Class<T> contract,
            EntrypointInvoker<T> invoker) {
        for (var container : BridgeFabricLoader.getInstance()
                .getEntrypointContainers(key, contract)) {
            if (container.getProvider() == bridgeModContainer) {
                T instance = container.getEntrypoint();
                rememberModInstance(instance);
                invoker.invoke(instance);
            }
        }
    }

    private synchronized void rememberModInstance(Object instance) {
        if (!modInstances.contains(instance)) {
            modInstances.add(instance);
        }
    }

    @Override
    protected <T extends Event & IModBusEvent> void acceptEvent(T event) {
        if (!active) {
            return;
        }
        FabricRegistrationLifecycle.invokeIfInitializationEvent(event);
        if (FabricClientModelRegistration.registerIfModelEvent(event)) {
            return;
        }
        if (FabricClientRecipeBookRegistration.registerIfEvent(event)) {
            return;
        }
    }

    @Override
    public boolean matches(Object mod) {
        return modInstances.contains(mod);
    }

    @Override
    public Object getMod() {
        return modInstances.isEmpty() ? this : modInstances.getFirst();
    }

    @FunctionalInterface
    private interface EntrypointInvoker<T> {
        void invoke(T entrypoint);
    }
}
