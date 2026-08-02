# Architecture

The engine is split so a future GUI can call the same Java API as the CLI.

| Module | Responsibility |
| --- | --- |
| `bridge-api` | Stable launcher-neutral adapter records and diagnostics |
| `repository-modrinth` | Modrinth v2 search, version metadata, dependencies, and verified artifact caching |
| `fabric-metadata` | Safe archive inspection and local dependency planning |
| `fabric-remap` | Reference analysis, TinyRemapper wrapper, deterministic preparation, lock/report writing |
| `fabric-loader-shim` | Independently implemented common Fabric Loader API surface |
| `forge-runtime` | Forge language provider and custom mod-container boundary |
| `forge-transform-service` | Early ModLauncher transformation-service boundary |
| `integration-harness` | Reusable Forge process launch, readiness, and clean-shutdown verification |
| `cli` | ServiceLoader discovery and machine-oriented commands/exit codes |
| `fixture-fabric-main` | Controlled Fabric `main` entrypoint probe |

Adapter discovery is exclusively through Java `ServiceLoader`. A future loader
direction can be added as another provider without editing the CLI.

The prepare cache key includes the source SHA-256, adapter version, Minecraft
version, Forge version, Mojang client/mapping checksums, and intermediary
mapping checksum. Output ZIP entries are sorted and assigned a fixed timestamp.
Source signatures are removed because any byte change makes them invalid.

## Runtime boundary

`fabricbridge` is registered as an `IModLanguageProvider`. It creates a custom
Forge `ModContainer` rather than synthesizing `@Mod` classes. The current
container can register and invoke ordinary `main` class entrypoints.

Forge 52.1.0 excludes any JAR advertising `ITransformationService` from normal
mod and language-provider discovery. LoaderBridge therefore packages the
language provider and transformation service separately. The transformation
service is deliberately empty until mixin configurations and access-widener
transformers can be registered early enough to be correct.
