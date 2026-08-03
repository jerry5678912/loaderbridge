package dev.loaderbridge.fabric.api.registry;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.event.registry.RegistryIdRemapCallback;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

/** Registry-local Fabric events fired by the early MappedRegistry hook. */
public final class RegistryEventDispatcher {
    private static final Map<Registry<?>, Events<?>> EVENTS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private RegistryEventDispatcher() {
    }

    public static <T> Event<RegistryEntryAddedCallback<T>> entryAdded(Registry<T> registry) {
        return events(registry).entryAdded;
    }

    public static <T> Event<RegistryIdRemapCallback<T>> remapped(Registry<T> registry) {
        return events(registry).remapped;
    }

    public static <T> void fireEntryAdded(Registry<T> registry, int rawId,
            ResourceLocation id, T value) {
        events(registry).entryAdded.invoker().onEntryAdded(rawId, id, value);
    }

    @SuppressWarnings("unchecked")
    private static <T> Events<T> events(Registry<T> registry) {
        synchronized (EVENTS) {
            return (Events<T>) EVENTS.computeIfAbsent(registry, ignored -> new Events<>());
        }
    }

    private static final class Events<T> {
        private final Event<RegistryEntryAddedCallback<T>> entryAdded = EventFactory.createArrayBacked(
                cast(RegistryEntryAddedCallback.class),
                callbacks -> (rawId, id, value) -> {
                    for (RegistryEntryAddedCallback<T> callback : callbacks) {
                        callback.onEntryAdded(rawId, id, value);
                    }
                });
        private final Event<RegistryIdRemapCallback<T>> remapped = EventFactory.createArrayBacked(
                cast(RegistryIdRemapCallback.class),
                callbacks -> state -> {
                    for (RegistryIdRemapCallback<T> callback : callbacks) {
                        callback.onRemap(state);
                    }
                });

        @SuppressWarnings("unchecked")
        private static <T> Class<T> cast(Class<?> type) {
            return (Class<T>) type;
        }
    }
}
