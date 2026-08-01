package net.fabricmc.loader.api;

public interface ObjectShare {
    Object get(String key);

    Object put(String key, Object value);

    Object putIfAbsent(String key, Object value);

    Object remove(String key);
}
