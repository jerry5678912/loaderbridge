# Architecture

The engine is split so a future GUI can call the same Java API as the CLI.

| Module | Responsibility |
| --- | --- |
| `bridge-api` | Stable launcher-neutral adapter records and diagnostics |
| `repository-modrinth` | Modrinth v2 search, version metadata, dependencies, and verified artifact caching |
| `repository-curseforge` | Authenticated CurseForge search, paginated file/dependency resolution, and verified artifact caching |
| `compatibility-catalog` | Deterministic ranking, cross-site deduplication, immutable snapshots, recursive locks, normalized input capture, and fail-closed offline replay |
| `scenario-api` | Versioned behavioral scenarios, bounded steps, plugins, results, and failure classification |
| `scenario-yaml` | Safe YAML 1.2 parsing into scenario contracts without arbitrary object construction |
| `fabric-metadata` | Safe archive inspection and local dependency planning |
| `fabric-remap` | Reference analysis, TinyRemapper wrapper, deterministic preparation, lock/report writing |
| `fabric-loader-shim` | Independently implemented common Fabric Loader API surface |
| `fabric-api-base-bridge` | Versioned Fabric API event and utility contracts selected from inspected references |
| `fabric-content-registries-v0-bridge` | Fabric fuel, composting, flammability, flattening, stripping, oxidation, and waxing registries mapped into vanilla/Forge behavior |
| `fabric-item-group-api-v1-bridge` | Fabric custom creative tabs and ordered item-entry callbacks mapped to Forge tab-content builds |
| `fabric-command-api-v2-bridge` | Fabric server command registration mapped to Forge's live dispatcher and build context |
| `fabric-lifecycle-events-bridge` | Complete Fabric lifecycle-events-v1 server/world/entity/block-entity/chunk/tick/tag surface mapped to Forge plus exact Mixin hooks |
| `fabric-resource-loader-v0-bridge` | Fabric resource listeners plus safe built-in pack discovery and Forge repository exposure |
| `fabric-game-rule-api-v1-bridge` | Fabric custom game-rule registration, types, visitors, callbacks, and persistence |
| `forge-runtime` | Forge language provider and custom mod-container boundary |
| `forge-transform-service` | Early ModLauncher transformation-service boundary |
| `integration-harness` | Pinned disposable Forge installation, scenario execution, reload, bounded commands, and artifacts |
| `cli` | ServiceLoader discovery and machine-oriented commands/exit codes |
| `fixture-fabric-main` | Controlled Fabric `main` entrypoint probe |
| `fixture-fabric-nested-child` | Fabric child mod embedded in the main fixture for recursive preprocessing and runtime containment probes |
| `fixture-fabric-api-base` | Controlled automatic API-module selection and runtime probe |
| `fixture-fabric-command` | Controlled Fabric callback registration and real Brigadier command execution probe |
| `fixture-fabric-lifecycle` | Controlled automatic lifecycle+base selection and ordered server tick probe |
| `fabric-registry-sync-v0-bridge` | Static Fabric custom-registry builders, attributes, and registry-local events over Forge synchronization |
| `fabric-screen-handler-api-v1-bridge` | Typed Fabric extended-menu opening data and client screen construction over Forge payloads and menus |
| `fabric-convention-tags-v2-bridge` | Complete pinned Fabric common-tag ABI, TagKey extensions, and standardized `c:` data merged with Forge resources |
| `fabric-tag-api-v1-bridge` | Fabric tag-file ABI and `fabric:remove` translation into Forge's ordered tag-removal engine |
| `fabric-dimensions-v1-bridge` | Fabric's success-producing fail-soft custom-dimension map codec and world-data compatibility Mixin |
| `dimensions-datafix-agent` | Isolated, relocated-ASM startup agent that preserves unknown dimension choices before Forge creates secure module layers |
| `dimensions-datafix-agent-provider` | ServiceLoader provider that installs the agent and declares its required JVM argument only when Dimensions is selected |

Adapter discovery is exclusively through Java `ServiceLoader`. A future loader
direction can be added as another provider without editing the CLI.
Runtime API bridges use a separate `RuntimeBridgeModuleProvider` service. Each
module advertises exact binary classes and Fabric mod versions; overlapping
unimplemented references remain gated instead of turning on a broad Fabric API
claim. Selected modules are copied into prepared output and locked by checksum.
Non-mod startup artifacts use `RuntimeLaunchArtifactProvider`. Their files and
JVM arguments are emitted into `loaderbridge.launch.json`, so a launcher can
activate selected early-runtime support without hardcoding module names.

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
