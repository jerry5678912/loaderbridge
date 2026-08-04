package dev.loaderbridge.forge;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraftforge.eventbus.api.Event;

/** Opens Forge's registry window before invoking Fabric's common entrypoints. */
final class FabricRegistrationLifecycle {
    private static final String CONSTRUCT_EVENT =
            "net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent";
    private static final String COMMON_SETUP_EVENT =
            "net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent";
    private static final String CLIENT_RECIPE_BOOK_EVENT =
            "net.minecraftforge.client.event.RegisterRecipeBookCategoriesEvent";
    private static final Coordinator MAIN_ENTRYPOINTS = new Coordinator();
    private static final PreLaunchCoordinator PRE_LAUNCH_ENTRYPOINTS =
            new PreLaunchCoordinator();
    private static final ServerCoordinator SERVER_ENTRYPOINTS = new ServerCoordinator();

    private FabricRegistrationLifecycle() {
    }

    static void registerPreLaunchEntrypoints(String modId, Runnable entrypoints) {
        PRE_LAUNCH_ENTRYPOINTS.register(modId, entrypoints);
    }

    static void registerMainEntrypoints(String modId, Runnable entrypoints) {
        MAIN_ENTRYPOINTS.registerMain(modId, entrypoints);
    }

    static void registerClientEntrypoints(String modId, Runnable entrypoints) {
        MAIN_ENTRYPOINTS.registerClient(modId, entrypoints);
    }

    static void registerServerEntrypoints(String modId, Runnable entrypoints) {
        SERVER_ENTRYPOINTS.register(modId, entrypoints);
    }

    static boolean invokeIfInitializationEvent(Event event) {
        String eventName = event.getClass().getName();
        boolean preLaunch = PRE_LAUNCH_ENTRYPOINTS.invokeIfConstructEvent(eventName);
        boolean server = SERVER_ENTRYPOINTS.invokeIfServerSetupEvent(eventName);
        boolean initialization = MAIN_ENTRYPOINTS.invokeIfInitializationEvent(eventName,
                () -> openForgeRegistryWindow(event),
                () -> {
                    if (CLIENT_RECIPE_BOOK_EVENT.equals(event.getClass().getName())) {
                        publishClientGameInstance(event.getClass().getClassLoader());
                    }
                    FabricClientModelRegistration.captureBeforeEntrypoints(event);
                    FabricClientRecipeBookRegistration.captureBeforeEntrypoints(event);
                },
                () -> {
                    FabricClientModelRegistration.captureAfterEntrypoints(event);
                    FabricClientRecipeBookRegistration.captureAfterEntrypoints(event);
                });
        return preLaunch || initialization || server;
    }

    static void publishClientGameInstance(ClassLoader gameClassLoader) {
        try {
            Class<?> minecraft = gameClassLoader.loadClass("net.minecraft.client.Minecraft");
            Object instance = minecraft.getMethod("getInstance").invoke(null);
            if (instance == null) {
                throw new IllegalStateException(
                        "LB-LOADER-GAME-001: Minecraft client instance is unavailable");
            }
            dev.loaderbridge.fabric.runtime.BridgeFabricLoader.getInstance()
                    .publishGameInstance(instance);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException exception) {
            throw new IllegalStateException(
                    "LB-LOADER-GAME-002: Minecraft client instance API is unavailable", exception);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException(
                    "LB-LOADER-GAME-003: Minecraft rejected client instance lookup",
                    exception.getCause());
        }
    }

    static final class PreLaunchCoordinator {
        private final List<RegisteredEntrypoints> pending = new ArrayList<>();
        private boolean invoked;

        synchronized void register(String modId, Runnable entrypoints) {
            if (invoked) {
                throw new IllegalStateException(
                        "LB-ENTRY-007: Fabric preLaunch entrypoint registered after construct");
            }
            pending.add(new RegisteredEntrypoints(modId, entrypoints));
        }

        synchronized boolean invokeIfConstructEvent(String eventName) {
            if (!CONSTRUCT_EVENT.equals(eventName) || invoked) {
                return false;
            }
            invoked = true;
            invokeInModOrder(pending);
            pending.clear();
            return true;
        }
    }

    static final class Coordinator {
        private final List<RegisteredEntrypoints> pendingMain = new ArrayList<>();
        private final List<RegisteredEntrypoints> pendingClient = new ArrayList<>();
        private boolean invoked;

        synchronized void registerMain(String modId, Runnable entrypoints) {
            if (invoked) {
                throw new IllegalStateException(
                        "LB-ENTRY-005: Fabric main entrypoint registered after common setup");
            }
            pendingMain.add(new RegisteredEntrypoints(modId, entrypoints));
        }

        synchronized void registerClient(String modId, Runnable entrypoints) {
            if (invoked) {
                throw new IllegalStateException(
                        "LB-ENTRY-006: Fabric client entrypoint registered after common setup");
            }
            pendingClient.add(new RegisteredEntrypoints(modId, entrypoints));
        }

        synchronized boolean invokeIfInitializationEvent(String eventName,
                Runnable openRegistryWindow) {
            return invokeIfInitializationEvent(
                    eventName, openRegistryWindow, () -> { }, () -> { });
        }

        synchronized boolean invokeIfInitializationEvent(String eventName,
                Runnable openRegistryWindow, Runnable beforeEntrypoints,
                Runnable afterEntrypoints) {
            boolean supportedEvent = COMMON_SETUP_EVENT.equals(eventName)
                    || CLIENT_RECIPE_BOOK_EVENT.equals(eventName);
            if (!supportedEvent || invoked) {
                return false;
            }
            invoked = true;
            openRegistryWindow.run();
            beforeEntrypoints.run();
            invokeInModOrder(pendingMain);
            invokeInModOrder(pendingClient);
            afterEntrypoints.run();
            pendingMain.clear();
            pendingClient.clear();
            return true;
        }
    }

    static final class ServerCoordinator {
        private static final String SERVER_SETUP_EVENT =
                "net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent";
        private final List<RegisteredEntrypoints> pending = new ArrayList<>();
        private boolean invoked;

        synchronized void register(String modId, Runnable entrypoints) {
            if (invoked) {
                throw new IllegalStateException(
                        "LB-ENTRY-008: Fabric server entrypoint registered after server setup");
            }
            pending.add(new RegisteredEntrypoints(modId, entrypoints));
        }

        synchronized boolean invokeIfServerSetupEvent(String eventName) {
            if (!SERVER_SETUP_EVENT.equals(eventName) || invoked) return false;
            invoked = true;
            invokeInModOrder(pending);
            pending.clear();
            return true;
        }
    }

    private static void invokeInModOrder(List<RegisteredEntrypoints> entrypoints) {
        entrypoints.stream().sorted(Comparator.comparing(RegisteredEntrypoints::modId))
                .map(RegisteredEntrypoints::action).forEach(Runnable::run);
    }

    private record RegisteredEntrypoints(String modId, Runnable action) {}

    private static void openForgeRegistryWindow(Event registerEvent) {
        try {
            ClassLoader forgeLoader = registerEvent.getClass().getClassLoader();
            Class<?> gameData = forgeLoader.loadClass("net.minecraftforge.registries.GameData");
            gameData.getMethod("unfreezeData").invoke(null);
            Class<?> managerType = forgeLoader.loadClass("net.minecraftforge.registries.RegistryManager");
            Object active = managerType.getField("ACTIVE").get(null);
            Map<?, ?> registries = (Map<?, ?>) managerType.getMethod("getRegistries").invoke(active);
            for (Object registry : registries.values()) {
                registry.getClass().getMethod("unfreeze").invoke(registry);
            }
        } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException
                | IllegalAccessException exception) {
            throw new IllegalStateException(
                    "LB-REGISTRY-001: Forge registry lifecycle API is unavailable", exception);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException(
                    "LB-REGISTRY-002: Forge rejected the Fabric registration window",
                    exception.getCause());
        }
    }
}
