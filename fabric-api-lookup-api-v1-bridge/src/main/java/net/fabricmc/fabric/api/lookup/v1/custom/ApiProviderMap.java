package net.fabricmc.fabric.api.lookup.v1.custom;

import dev.loaderbridge.fabric.api.lookup.IdentityApiProviderMap;

/** Thread-safe identity-keyed provider map. */
public interface ApiProviderMap<K, V> {
    static <K, V> ApiProviderMap<K, V> create() {
        return new IdentityApiProviderMap<>();
    }

    V get(K key);

    V putIfAbsent(K key, V provider);
}
