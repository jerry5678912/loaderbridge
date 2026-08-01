package dev.loaderbridge.api;

import java.util.Locale;
import java.util.Objects;

/** Stable, extensible identifier for a mod loader. */
public record LoaderId(String value) {
    public LoaderId {
        Objects.requireNonNull(value, "value");
        value = value.trim().toLowerCase(Locale.ROOT);
        if (!value.matches("[a-z][a-z0-9_-]{1,63}")) {
            throw new IllegalArgumentException("Invalid loader id: " + value);
        }
    }
}
