package dev.loaderbridge.fabric.api.base;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.resources.ResourceLocation;

/** Thread-safe event implementation preserving Fabric phase and listener ordering. */
public final class BridgeArrayBackedEvent<T> extends Event<T> {
    private final Function<T[], T> invokerFactory;
    private final Class<?> listenerType;
    private final Object lock = new Object();
    private final Map<ResourceLocation, List<T>> listeners = new LinkedHashMap<>();
    private final Map<ResourceLocation, Set<ResourceLocation>> ordering = new LinkedHashMap<>();

    public BridgeArrayBackedEvent(Class<? super T> type, Function<T[], T> invokerFactory) {
        listenerType = Objects.requireNonNull(type, "type");
        this.invokerFactory = Objects.requireNonNull(invokerFactory, "invokerFactory");
        ensurePhase(DEFAULT_PHASE);
        rebuildInvoker();
    }

    @Override
    public void register(T listener) {
        register(DEFAULT_PHASE, listener);
    }

    @Override
    public void register(ResourceLocation phase, T listener) {
        Objects.requireNonNull(phase, "Tried to register a listener for a null phase!");
        Objects.requireNonNull(listener, "Tried to register a null listener!");
        synchronized (lock) {
            ensurePhase(phase).add(listener);
            rebuildInvokerLocked();
        }
    }

    @Override
    public void addPhaseOrdering(ResourceLocation firstPhase, ResourceLocation secondPhase) {
        Objects.requireNonNull(firstPhase, "Tried to add an ordering for a null phase.");
        Objects.requireNonNull(secondPhase, "Tried to add an ordering for a null phase.");
        if (firstPhase.equals(secondPhase)) {
            throw new IllegalArgumentException("Tried to add a phase that depends on itself.");
        }
        synchronized (lock) {
            ensurePhase(firstPhase);
            ensurePhase(secondPhase);
            ordering.get(firstPhase).add(secondPhase);
            rebuildInvokerLocked();
        }
    }

    public void rebuildInvoker() {
        synchronized (lock) {
            rebuildInvokerLocked();
        }
    }

    private List<T> ensurePhase(ResourceLocation phase) {
        ordering.computeIfAbsent(phase, ignored -> new LinkedHashSet<>());
        return listeners.computeIfAbsent(phase, ignored -> new ArrayList<>());
    }

    private void rebuildInvokerLocked() {
        List<T> flattened = new ArrayList<>();
        for (ResourceLocation phase : sortedPhases()) flattened.addAll(listeners.get(phase));
        @SuppressWarnings("unchecked")
        T[] array = (T[]) Array.newInstance(listenerType, flattened.size());
        invoker = invokerFactory.apply(flattened.toArray(array));
    }

    private List<ResourceLocation> sortedPhases() {
        Map<ResourceLocation, Integer> incoming = new LinkedHashMap<>();
        listeners.keySet().forEach(phase -> incoming.put(phase, 0));
        ordering.values().forEach(targets -> targets.forEach(target ->
                incoming.compute(target, (ignored, count) -> count == null ? 1 : count + 1)));
        PriorityQueue<ResourceLocation> ready = new PriorityQueue<>(Comparator.comparing(ResourceLocation::toString));
        incoming.forEach((phase, count) -> {
            if (count == 0) ready.add(phase);
        });
        List<ResourceLocation> result = new ArrayList<>(incoming.size());
        while (!ready.isEmpty()) {
            ResourceLocation phase = ready.remove();
            result.add(phase);
            for (ResourceLocation target : ordering.getOrDefault(phase, Set.of())) {
                int remaining = incoming.computeIfPresent(target, (ignored, count) -> count - 1);
                if (remaining == 0) ready.add(target);
            }
        }
        if (result.size() != incoming.size()) {
            incoming.keySet().stream().filter(phase -> !result.contains(phase))
                    .sorted(Comparator.comparing(ResourceLocation::toString)).forEach(result::add);
        }
        return result;
    }
}
