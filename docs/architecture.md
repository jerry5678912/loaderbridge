# Architecture

The engine is split so a future GUI can call the same Java API as the CLI.

| Module | Responsibility |
| --- | --- |
| `bridge-api` | Stable launcher-neutral adapter records and diagnostics |
| `fabric-metadata` | Safe archive inspection and local dependency planning |
| `fabric-remap` | Reference analysis, TinyRemapper wrapper, deterministic preparation, lock/report writing |
| `fabric-loader-shim` | Independently implemented common Fabric Loader API surface |
| `forge-runtime` | Forge language provider, transformation service, and mod-container boundary |
| `cli` | ServiceLoader discovery and machine-oriented commands/exit codes |

Adapter discovery is exclusively through Java `ServiceLoader`. A future loader
direction can be added as another provider without editing the CLI.

The prepare cache key currently includes the source SHA-256, adapter version,
Minecraft version, and Forge version. Mapping checksums will join the key when
the upstream resolver is connected. Output ZIP entries are sorted and assigned
a fixed timestamp. Source signatures are removed because any byte change makes
them invalid.

## Runtime boundary

`fabricbridge` is registered as an `IModLanguageProvider`. It creates a custom
Forge `ModContainer` rather than synthesizing `@Mod` classes. The current
container can register and invoke ordinary `main` class entrypoints. The
transformation service is deliberately empty until mixin configurations and
access-widener transformers can be registered early enough to be correct.
