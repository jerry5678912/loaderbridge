package dev.loaderbridge.fabric.api.lookup;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import net.fabricmc.fabric.api.lookup.v1.custom.ApiProviderMap;

/** Copy-on-write identity map with lock-free reads. */
public final class IdentityApiProviderMap<K, V> implements ApiProviderMap<K, V> {
    private volatile Map<K, V> snapshot = new IdentityHashMap<>();

    @Override public V get(K key) { return snapshot.get(Objects.requireNonNull(key)); }

    @Override
    public synchronized V putIfAbsent(K key, V provider) {
        Objects.requireNonNull(key); Objects.requireNonNull(provider);
        V current = snapshot.get(key);
        if (current != null) return current;
        Map<K, V> copy = new IdentityHashMap<>(snapshot);
        copy.put(key, provider);
        snapshot = copy;
        return null;
    }
}
