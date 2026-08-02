package dev.loaderbridge.api;

/** Independently reportable pieces of bridge functionality. */
public enum BridgeCapability {
    METADATA,
    DEPENDENCY_RESOLUTION,
    REMAPPING,
    MIXINS,
    MIXIN_EXTRAS,
    ACCESS_WIDENERS,
    NESTED_JARS,
    LOADER_API,
    FABRIC_API
}
