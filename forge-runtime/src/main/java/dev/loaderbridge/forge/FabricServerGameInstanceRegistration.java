package dev.loaderbridge.forge;

import dev.loaderbridge.fabric.runtime.BridgeFabricLoader;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Publishes the dedicated-server instance at Forge's post-construction lifecycle boundary. */
final class FabricServerGameInstanceRegistration {
    private static final AtomicBoolean INSTALLED = new AtomicBoolean();

    private FabricServerGameInstanceRegistration() {}

    static void install(ClassLoader gameClassLoader) {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }
        try {
            Class<?> minecraftForge = gameClassLoader.loadClass(
                    "net.minecraftforge.common.MinecraftForge");
            Object eventBus = minecraftForge.getField("EVENT_BUS").get(null);
            Class<?> eventBusType = gameClassLoader.loadClass(
                    "net.minecraftforge.eventbus.api.IEventBus");
            Class<?> priorityType = gameClassLoader.loadClass(
                    "net.minecraftforge.eventbus.api.EventPriority");
            Class<?> serverEvent = gameClassLoader.loadClass(
                    "net.minecraftforge.event.server.ServerAboutToStartEvent");
            @SuppressWarnings({"rawtypes", "unchecked"})
            Object normalPriority = Enum.valueOf((Class<? extends Enum>) priorityType, "NORMAL");
            Consumer<Object> listener = FabricServerGameInstanceRegistration::publishServerInstance;
            eventBusType.getMethod("addListener", priorityType, boolean.class,
                    Class.class, Consumer.class)
                    .invoke(eventBus, normalPriority, false, serverEvent, listener);
        } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException
                | IllegalAccessException exception) {
            INSTALLED.set(false);
            throw new IllegalStateException(
                    "LB-LOADER-GAME-004: Forge server lifecycle API is unavailable", exception);
        } catch (InvocationTargetException exception) {
            INSTALLED.set(false);
            throw new IllegalStateException(
                    "LB-LOADER-GAME-005: Forge rejected server instance registration",
                    exception.getCause());
        }
    }

    static void publishServerInstance(Object event) {
        try {
            Object server = event.getClass().getMethod("getServer").invoke(event);
            BridgeFabricLoader.getInstance().publishGameInstance(server);
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new IllegalStateException(
                    "LB-LOADER-GAME-006: Forge server event has no accessible instance", exception);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException(
                    "LB-LOADER-GAME-007: Forge rejected server instance lookup",
                    exception.getCause());
        }
    }
}
