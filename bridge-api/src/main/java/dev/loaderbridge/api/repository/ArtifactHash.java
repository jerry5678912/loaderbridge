package dev.loaderbridge.api.repository;

import java.util.Locale;
import java.util.Objects;

public record ArtifactHash(HashAlgorithm algorithm, String value) {
    public ArtifactHash {
        Objects.requireNonNull(algorithm, "algorithm");
        Objects.requireNonNull(value, "value");
        value = value.toLowerCase(Locale.ROOT);
        if (value.length() != algorithm.hexadecimalLength() || !value.matches("[0-9a-f]+")) {
            throw new IllegalArgumentException("Invalid " + algorithm + " hash");
        }
    }
}
