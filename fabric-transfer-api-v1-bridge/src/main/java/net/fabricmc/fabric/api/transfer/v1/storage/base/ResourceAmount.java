package net.fabricmc.fabric.api.transfer.v1.storage.base;

/** Immutable resource and amount pair. */
public record ResourceAmount<T>(T resource, long amount) {
}
