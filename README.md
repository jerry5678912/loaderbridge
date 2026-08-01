# LoaderBridge

LoaderBridge is an experimental Java 21 compatibility engine for preparing
Fabric 1.21.1 mods to run on Forge 1.21.1. It is a command-line scaffold, not a
finished launcher, and it does not claim universal mod compatibility.

The engine inspects JAR metadata, plans a directional bridge, prepares a new
artifact without modifying the source, and emits machine-readable diagnostics.

## Status

Work in progress. The metadata and preparation pipeline can be used without
executing untrusted mod classes. Forge runtime integration is intentionally
isolated behind service-provider contracts.

## Build

```shell
./gradlew build
```

Java 21 is required.
