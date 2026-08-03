package net.fabricmc.fabric.api.event;

import dev.loaderbridge.fabric.api.base.BridgeArrayBackedEvent;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;

/**
 * Fabric API 0.4.42 event factories, independently implemented for LoaderBridge.
 * Contract source: https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api-base/0.4.42%2B6573ed8c19/
 */
public final class EventFactory {
    private static final Set<BridgeArrayBackedEvent<?>> EVENTS =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private EventFactory() {}

    public static <T> Event<T> createArrayBacked(Class<? super T> type,
            Function<T[], T> invokerFactory) {
        BridgeArrayBackedEvent<T> event = new BridgeArrayBackedEvent<>(type, invokerFactory);
        EVENTS.add(event);
        return event;
    }

    public static <T> Event<T> createArrayBacked(Class<T> type, T emptyInvoker,
            Function<T[], T> invokerFactory) {
        return createArrayBacked(type, listeners -> {
            if (listeners.length == 0) return emptyInvoker;
            if (listeners.length == 1) return listeners[0];
            return invokerFactory.apply(listeners);
        });
    }

    public static <T> Event<T> createWithPhases(Class<? super T> type,
            Function<T[], T> invokerFactory, ResourceLocation... defaultPhases) {
        boolean containsDefault = false;
        for (int first = 0; first < defaultPhases.length; first++) {
            if (Event.DEFAULT_PHASE.equals(defaultPhases[first])) containsDefault = true;
            for (int second = first + 1; second < defaultPhases.length; second++) {
                if (defaultPhases[first].equals(defaultPhases[second])) {
                    throw new IllegalArgumentException("Duplicate event phase: " + defaultPhases[first]);
                }
            }
        }
        if (!containsDefault) {
            throw new IllegalArgumentException("The event phases must contain Event.DEFAULT_PHASE.");
        }
        Event<T> event = createArrayBacked(type, invokerFactory);
        for (int index = 1; index < defaultPhases.length; index++) {
            event.addPhaseOrdering(defaultPhases[index - 1], defaultPhases[index]);
        }
        return event;
    }

    @Deprecated
    public static String getHandlerName(Object handler) {
        return handler.getClass().getName();
    }

    @Deprecated
    public static boolean isProfilingEnabled() {
        return false;
    }

    @Deprecated(forRemoval = true)
    public static void invalidate() {
        synchronized (EVENTS) {
            EVENTS.forEach(BridgeArrayBackedEvent::rebuildInvoker);
        }
    }
}
