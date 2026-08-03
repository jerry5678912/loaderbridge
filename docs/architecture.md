# Architecture

The engine is split so a future GUI can call the same Java API as the CLI.

| Module | Responsibility |
| --- | --- |
| `bridge-api` | Stable launcher-neutral adapter records and diagnostics |
| `repository-modrinth` | Modrinth v2 search, version metadata, dependencies, and verified artifact caching |
| `repository-curseforge` | Authenticated CurseForge search, paginated file/dependency resolution, and verified artifact caching |
| `compatibility-catalog` | Deterministic ranking, cross-site deduplication, immutable snapshots, and canonical serialization |
| `scenario-api` | Versioned behavioral scenarios, bounded steps, plugins, results, and failure classification |
| `scenario-yaml` | Safe YAML 1.2 parsing into scenario contracts without arbitrary object construction |
| `fabric-metadata` | Safe archive inspection and local dependency planning |
| `fabric-remap` | Reference analysis, TinyRemapper wrapper, deterministic preparation, lock/report writing |
| `fabric-loader-shim` | Independently implemented common Fabric Loader API surface |
| `fabric-api-base-bridge` | Versioned Fabric API event and utility contracts selected from inspected references |
| `fabric-command-api-v2-bridge` | Fabric server command registration mapped to Forge's live dispatcher and build context |
| `fabric-lifecycle-events-bridge` | Fabric server lifecycle/world/entity/tick/tag events mapped to Forge plus exact Mixin hooks |
| `forge-runtime` | Forge language provider and custom mod-container boundary |
| `forge-transform-service` | Early ModLauncher transformation-service boundary |
| `integration-harness` | Pinned disposable Forge installation, scenario execution, reload, bounded commands, and artifacts |
| `cli` | ServiceLoader discovery and machine-oriented commands/exit codes |
| `fixture-fabric-main` | Controlled Fabric `main` entrypoint probe |
| `fixture-fabric-api-base` | Controlled automatic API-module selection and runtime probe |
| `fixture-fabric-command` | Controlled Fabric callback registration and real Brigadier command execution probe |
| `fixture-fabric-lifecycle` | Controlled automatic lifecycle+base selection and ordered server tick probe |

Adapter discovery is exclusively through Java `ServiceLoader`. A future loader
direction can be added as another provider without editing the CLI.
Runtime API bridges use a separate `RuntimeBridgeModuleProvider` service. Each
module advertises exact binary classes and Fabric mod versions; overlapping
unimplemented references remain gated instead of turning on a broad Fabric API
claim. Selected modules are copied into prepared output and locked by checksum.

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
language provider and transformation service separately. During discovery, the
transformation service scans only translated JARs declaring LoaderBridge access
wideners, validates bounded archive resources, merges their rules, and registers
a ModLauncher transformer before target classes are defined.

Mixin configuration resources are registered through Forge's existing Mixin
0.8.7 installation. Preprocessing maps intermediary targets and selectors to
the official runtime namespace, disables redundant runtime remapping, and
applies versioned structural repairs only when bytecode and mapping predicates
match. No second Mixin runtime is installed.
