package dev.loaderbridge.fabric.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import net.fabricmc.loader.api.ObjectShare;

final class BridgeObjectShare implements ObjectShare {
    // Contract: https://github.com/FabricMC/fabric-loader/blob/0.16.14/src/main/java/net/fabricmc/loader/impl/ObjectShareImpl.java
    private final Map<String, Object> values = new HashMap<>();
    private final Map<String, List<BiConsumer<String, Object>>> pending = new HashMap<>();

    @Override
    public synchronized Object get(String key) {
        validateKey(key);
        return values.get(key);
    }

    @Override
    public void whenAvailable(String key, BiConsumer<String, Object> consumer) {
        validateKey(key);
        Objects.requireNonNull(consumer, "null consumer");
        Object value;
        synchronized (this) {
            value = values.get(key);
            if (value == null) {
                pending.computeIfAbsent(key, ignored -> new ArrayList<>()).add(consumer);
                return;
            }
        }
        consumer.accept(key, value);
    }

    @Override
    public Object put(String key, Object value) {
        return put(key, value, false);
    }

    @Override
    public Object putIfAbsent(String key, Object value) {
        return put(key, value, true);
    }

    private Object put(String key, Object value, boolean onlyIfAbsent) {
        validateKey(key);
        Objects.requireNonNull(value, "null value");
        List<BiConsumer<String, Object>> consumers;
        synchronized (this) {
            Object previous = onlyIfAbsent ? values.putIfAbsent(key, value) : values.put(key, value);
            if (previous != null) {
                return previous;
            }
            consumers = pending.remove(key);
        }
        if (consumers != null) {
            consumers.forEach(consumer -> consumer.accept(key, value));
        }
        return null;
    }

    @Override
    public synchronized Object remove(String key) {
        validateKey(key);
        return values.remove(key);
    }

    synchronized void clear() {
        values.clear();
        pending.clear();
    }

    private static void validateKey(String key) {
        Objects.requireNonNull(key, "null key");
        int separator = key.indexOf(':');
        if (separator <= 0 || separator == key.length() - 1) {
            throw new IllegalArgumentException("invalid key, must be modid:subkey");
        }
    }
}
