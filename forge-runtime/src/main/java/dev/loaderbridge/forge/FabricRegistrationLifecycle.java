package dev.loaderbridge.forge;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.eventbus.api.Event;

/** Opens Forge's registry window before invoking Fabric's common entrypoints. */
final class FabricRegistrationLifecycle {
    private static final String COMMON_SETUP_EVENT =
            "net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent";
    private static final Coordinator MAIN_ENTRYPOINTS = new Coordinator();

    private FabricRegistrationLifecycle() {
    }

    static void registerMainEntrypoints(Runnable entrypoints) {
        MAIN_ENTRYPOINTS.registerMain(entrypoints);
    }

    static void registerClientEntrypoints(Runnable entrypoints) {
        MAIN_ENTRYPOINTS.registerClient(entrypoints);
    }

    static boolean invokeIfCommonSetupEvent(Event event) {
        return MAIN_ENTRYPOINTS.invokeIfCommonSetupEvent(event.getClass().getName(),
                () -> openForgeRegistryWindow(event),
                () -> FabricClientModelRegistration.captureBeforeEntrypoints(event),
                () -> FabricClientModelRegistration.captureAfterEntrypoints(event));
    }

    static final class Coordinator {
        private final List<Runnable> pendingMain = new ArrayList<>();
        private final List<Runnable> pendingClient = new ArrayList<>();
        private boolean invoked;

        synchronized void registerMain(Runnable entrypoints) {
            if (invoked) {
                throw new IllegalStateException(
                        "LB-ENTRY-005: Fabric main entrypoint registered after common setup");
            }
            pendingMain.add(entrypoints);
        }

        synchronized void registerClient(Runnable entrypoints) {
            if (invoked) {
                throw new IllegalStateException(
                        "LB-ENTRY-006: Fabric client entrypoint registered after common setup");
            }
            pendingClient.add(entrypoints);
        }

        synchronized boolean invokeIfCommonSetupEvent(String eventName,
                Runnable openRegistryWindow) {
            return invokeIfCommonSetupEvent(eventName, openRegistryWindow, () -> { }, () -> { });
        }

        synchronized boolean invokeIfCommonSetupEvent(String eventName,
                Runnable openRegistryWindow, Runnable beforeEntrypoints,
                Runnable afterEntrypoints) {
            if (!COMMON_SETUP_EVENT.equals(eventName) || invoked) {
                return false;
            }
            invoked = true;
            openRegistryWindow.run();
            beforeEntrypoints.run();
            pendingMain.forEach(Runnable::run);
            pendingClient.forEach(Runnable::run);
            afterEntrypoints.run();
            pendingMain.clear();
            pendingClient.clear();
            return true;
        }
    }

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
