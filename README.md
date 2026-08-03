# LoaderBridge

LoaderBridge is an Apache-2.0 Java 21 command-line scaffold for a directional
Fabric 1.21.1 to Forge 52.1.x compatibility engine. It is not a launcher and it
does not claim that arbitrary Fabric mods run on Forge yet.

## What works now

- Content-based `fabric.mod.json` discovery, including recursively declared JARs.
- Bounded archive reads, ZIP-path and duplicate rejection, and no preprocessing
  classloading.
- Schema-v1 metadata, entrypoint, environment, dependency, alias, conflict,
  mixin, access-widener, nested-JAR, and language-adapter inspection.
- Local Fabric-style dependency diagnostics and common extended-semver ranges.
- ASM reference inventory for Loader API, Fabric API, Minecraft, reflection-like
  strings, and native libraries.
- Checksum-verified Mojang 1.21.1 client artifacts plus bundled Fabric
  intermediary mappings, composed into an intermediary-to-official remap.
- A real TinyRemapper pipeline for classes, fields, methods, descriptors, and
  lambdas, tested against the Minecraft 1.21.1 client JAR.
- Deterministic unsigned output JARs with generated `mods.toml` and
  `loaderbridge.json`, valid resource-pack metadata, implementation-fingerprinted
  SHA-256 caching, `bridge.lock.json`, and `compatibility-report.json`.
- Recursive nested-JAR transformation and loading with content and mod-ID/version
  deduplication plus preserved containment provenance.
- A ServiceLoader-discovered `BridgeAdapter`, a Forge language provider, a
  separate early transformation service, custom Forge mod container, and a
  small independently implemented Fabric Loader API shim.
- Native execution of ordinary Fabric `preLaunch`, `main`, `client`, and
  `server` entrypoints on Forge, including default member-style and Kotlin
  language-adapter declarations. All active definitions are installed before
  `preLaunch` callbacks run together at Forge construct; common/client
  initialization remains after registry population.
- Fabric Loader 0.16 runtime contracts for containers, rich metadata, versions,
  dependency predicates, aliases, object sharing, classpath roots, environment
  filtering, raw game version, arbitrary lazily resolved entrypoint containers,
  per-requested-type instance caching and aggregated failures, and
  intermediary-to-runtime `MappingResolver` lookups.
- Early registration of universal Fabric Mixin configurations through Forge's
  existing Mixin 0.8.7 runtime, proven by a controlled injection into a real
  Minecraft server class on dedicated and integrated servers.
- Intermediary Mixin target and injection-selector remapping through
  TinyRemapper's Mixin extension, including descriptor-qualified selectors.
- Deterministic generated wrappers for Fabric `client` and `server` scoped
  Mixin configs; original configs remain untouched while common declarations
  are exposed only on the declared Mixin side.
- Namespace-aware Mixin refmap translation for intermediary inputs, retaining
  both original and post-remap selector keys and translating owners, fields,
  methods, and nested descriptors across default and contextual mappings.
- Golden remapping coverage for all standard injection annotation families,
  nested `@At` member targets, accessors, invokers, shadows, and overwrites.
- Standard Mixin config plugins proven through real `IMixinConfigPlugin`
  callbacks on both launches of the controlled Forge server save/reload run.
- Annotation-aware MixinExtras detection with automatic, checksum-pinned
  installation of the official Forge game-library artifact; a controlled
  `@ModifyReturnValue` passed both server launches, save, and reload.
- Offline Mixin repair for official-runtime selectors, shared refmaps, nested
  string targets, `@Shadow` members, and safely relocatable constructor hooks.
  Rules match bytecode and mapping structure rather than mod IDs or filenames.
- Fabric access-widener v1/v2 remapping and early Forge transformation for
  accessible, extendable, mutable, and transitive class/member rules, with
  bounded resource loading and stable malformed-target diagnostics.
- A separately versioned Fabric API base bridge matching
  `fabric-api-base:0.4.42+6573ed8c19`: events, ordered phases, TriState, and
  utility constants. ServiceLoader selection is driven by bytecode references
  and declared dependencies, then recorded in the lock and report.
- A Content Registries v0 bridge matching
  `fabric-content-registries-v0:8.0.19+b559734419`. It implements Fabric's
  item/block override maps plus fuel, composting, flammability, shovel
  flattening, axe stripping, oxidation, and waxing registries. Automatic
  selection is driven by inspected classes and declared dependencies; early
  Mixins feed the custom transformations into Minecraft's vanilla lookup paths.
- An Item Group API v1 bridge matching
  `fabric-item-group-api-v1:4.1.7+def88e3a19`. Fabric custom-tab builders and
  keyed/global entry callbacks run through Forge's creative-tab build event,
  preserving parent/search visibility, feature filtering, prepend, and
  before/after ordering. The graphical fixture rebuilds its custom tab after
  each world join and observes the Fabric-added item on both runs.
- A separately versioned lifecycle bridge matching the complete server-tick,
  common tag-loading, server lifecycle, world, entity, block-entity, and chunk surfaces of
  `fabric-lifecycle-events-v1:2.6.0+0865547519`. It maps Fabric server/world
  tick, tag, server state, world/entity/block-entity/chunk load and unload,
  chunk generation and full-status transitions, equipment,
  datapack sync, reload, and save callbacks through
  Forge events plus a version-pinned early Mixin, and automatically pulls in
  its API-base dependency.
- A separately versioned Command API v2 bridge matching
  `fabric-command-api-v2:2.2.28+6ced4dd919`. Fabric server command callbacks
  receive Forge's live dispatcher, registry build context, and dedicated or
  integrated command environment without wrapper substitution.
- A separately versioned Resource Loader v0 bridge matching
  `fabric-resource-loader-v0:1.3.1+5b5275af19`. Server-data listeners and
  registry-aware factories preserve Fabric IDs and dependency order through
  initial load, datapack reload, save, and process restart.
- A separately versioned Game Rule API v1 bridge matching
  `fabric-game-rule-api-v1:1.0.53+6ced4dd919`. Boolean, bounded integer,
  double, enum, visitor, callback, custom-category, command, serialization,
  save, and process-restart persistence behavior use Minecraft's native rule
  registry in Forge's transformed game layer.
- A separately versioned Networking API v1 play bridge matching
  `fabric-networking-api-v1:4.3.1+d30f6a7919`. It implements payload codecs,
  global and connection-scoped server receivers, client receivers, Fabric
  packet senders, channel queries, server play and server configuration
  connection events, exact player lookup collections, bidirectional
  configuration receivers/sending, and entity start/stop tracking callbacks over
  Forge's payload channel without replacing Fabric payload objects in mod code.
- A separately versioned Object Builder API v1 bridge matching
  `fabric-object-builder-api-v1:15.2.1+40875a9319`. It currently implements
  `FabricBlockEntityTypeBuilder`, `FabricDefaultAttributeRegistry`, and the
  deprecated `FabricEntityTypeBuilder` base/living/mob contracts and the modern
  `FabricEntityType.Builder` interface injected into Minecraft's builder.
  Entity dimensions, tracking, velocity updates, feature requirements,
  attributes, Fabric's unnamed build behavior, and spawn restrictions are
  translated to Minecraft/Forge behavior. `BlockSetTypeBuilder` and
  `WoodTypeBuilder` also construct and register native behavior records with
  Fabric-compatible copying and defaults. Villager, wandering-trader, and
  rebalanced-pool registrations are translated through Forge's trade build
  events. Fabric common entrypoints run
  sequentially in resolved order inside Forge's registration window.
- A separately versioned API Lookup v1 bridge matching
  `fabric-api-lookup-api-v1:1.6.72+d30f6a7919`. It implements unique typed
  lookups; block, item, and entity direct/self/fallback providers; live block
  cache queries; and the public custom lookup/provider maps. Mods select it by
  bytecode or metadata and automatically receive its base and lifecycle bridge
  dependencies.
- A Registry Sync v0 bridge matching
  `fabric-registry-sync-v0:5.1.3+60c3209b19`. It implements static custom
  registry builders, registry attributes, entry-added/remap event contracts,
  and codec-backed dynamic registries loaded from datapacks and synchronized to
  clients. Setup callbacks expose each mutable data-loading layer, and
  `SKIP_WHEN_EMPTY` filters registry payload and tag synchronization. Forge owns
  the connection registry handshake; the controlled client proves static and
  dynamic registration through save and reload.
- A Convention Tags v2 bridge matching
  `fabric-convention-tags-v2:2.12.0+c3656daa19`, with the standard `c:` biome
  tag keys used by Fabric 1.21.1 mods.
- An initial Biome API v1 bridge matching
  `fabric-biome-api-v1:13.0.31+d527f9fd19`. Fabric biome selectors and placed
  feature additions are translated into a codec-backed Forge biome modifier.
- Initial Block Render Layer v1 and Rendering v1 bridges matching
  `fabric-blockrenderlayer-v1:1.1.52+0af3f5a719` and
  `fabric-rendering-v1:5.1.0+ab4c25a019`. They cover block render layers,
  block/item colors, entity and block-entity renderers, entity model layers,
  and late Fabric model-layer baking through Forge's existing Mixin runtime.
- An initial Transfer API v1 bridge matching
  `fabric-transfer-api-v1:5.4.4+7b3d111d19`. Its transaction foundation
  implements thread-confined outer and nested scopes, LIFO close callbacks,
  rollback and commit propagation, outer-close notifications, lifecycle state,
  and snapshot participants. Automatic bytecode selection and the graphical
  client prove nested rollback and final commit during Fabric initialization.
  The generic `Storage`/`StorageView` core adds empty storage, capability flags,
  filtered non-empty iteration, version guards, and transactional insert/extract;
  common composition types add slotted/single-slot views, ordered combined
  storage, insertion/extraction restrictions, filtered wrappers, transfer
  variants, resource amounts, and precondition checks. The client verifies
  aborted and committed mutations plus multi-slot routing before world loading.
  Storage utilities add atomic moves, simulation, stacking, resource queries,
  and comparator output; single-variant storage adds capacity, snapshot, and
  NBT codec behavior. A real client utility move passes before world creation.
  Item variants, single-stack/item storage, and vanilla inventory wrapping add
  component-aware stack identity, sided slot rules, capacity limits, rollback,
  commit, and dirty notification. The graphical client verifies a 70-item
  two-slot split and committed extraction before creating and reloading a world.
  `ItemStorage.SIDED` now discovers vanilla container block entities through the
  standard Fabric block lookup, with automatic lookup-module dependencies. A
  real chest lookup and insertion passes on initial load and world reload.
  Player inventory and container-item contexts add native stacking order,
  transactional hand/cursor mutation, overflow routing, and immutable
  simulation. The graphical client performs a hand exchange and cursor insert
  on both initial join and world reload.
- Side-aware scenario sessions using fixed server/client launch scripts, with
  bounded console commands, clean shutdown, reload, and artifact collection.
- ServiceLoader-discovered Modrinth and authenticated CurseForge providers with
  ranked search, release/dependency resolution, checksums, and verified caches.
- Deterministic catalog snapshots with platform quotas, cross-site hash/source
  deduplication, top-up enforcement, and canonical JSON serialization.
- Recursive required-dependency graph resolution, including Modrinth's
  version-only pins, deterministic installation ordering, and cycle detection.
- Versioned M1 behavioral-scenario contracts and strict bounded YAML parsing,
  with dynamically namespaced actions for future ServiceLoader plugins.
- A checked-in 25-scenario controlled M1 suite covering server and client
  lifecycle plus semantic game-state assertion families.
- A standalone ForgeGradle client laboratory that launches Forge 52.1.0,
  reaches the real title screen, opens or creates a disposable world, saves all
  dimensions, disconnects, reloads the world, and stops cleanly.
- Pinned real-mod probes for Lithium 0.14.3 and FerriteCore 7.0.3. Both pass
  client and dedicated-server ready, world save, clean shutdown, and reload on
  Forge 52.1.0 and 52.1.16 in the current macOS laboratory.
- Blockus `2.9.18+1.21.1` is the first passing pinned Fabric-only content
  probe. Official Modrinth metadata identifies version `BduJoveu` as Fabric and
  returns no Forge 1.21.1 release for the same project. On Forge 52.1.0 the
  deep graphical run registered `blockus:amethyst_bricks`, placed it, added its
  item to the player inventory, saved, reloaded, verified both persisted, and
  stopped cleanly. See [the pinned evidence](docs/real-mod-probes/blockus-2.9.18-1.21.1.md).
- Oxidized `1.8.4` is the next pinned Fabric-only technology/content probe.
  A generic Fabric-patched Minecraft ABI repair lets its SafLib dependency
  register the real `oxidized:copper_kiln` block entity on Forge. Registration,
  block and inventory model/texture resolution, placement, clay-and-coal kiln
  processing into a brick, machine-output persistence, save, and reload pass.
  A generic late-item model bridge loads and caches models for items registered
  by Fabric entrypoints after Forge constructs its item renderer. A generic
  cooking-recipe bridge also maps newly registered Fabric cooking types to
  Forge's vanilla food, block, or miscellaneous recipe-book categories. See
  [the current evidence](docs/real-mod-probes/oxidized-1.8.4.md).
- A controlled Fabric API base fixture passes automatic module installation,
  dedicated-server ready, world save, clean shutdown, and reload on Forge
  52.1.16.
- A controlled Fabric lifecycle fixture passes automatic base+lifecycle module
  installation, ordered ticks, world/entity/block-entity/chunk load/unload,
  fresh chunk generation and full-status transitions, equipment
  changes, tag loading, server state,
  datapack reload, before/after save, dedicated-server ready, clean shutdown,
  and process reload on Forge 52.1.16.
  The same fixture automatically selects Resource Loader v0 and proves its
  listener on initial server resources, `/reload`, and the restarted process.
  It also registers a Fabric custom game rule, changes it through the native
  command, saves it, and verifies the value after a full JVM restart.
  In the graphical Forge 52.1.0 lab it additionally completes a real
  server-to-client `ping` and client-to-server `pong` in both the initial and
  reloaded integrated world, registers a Fabric-built block-entity type and a
  custom living entity with default attributes, plus a custom mob with a native
  spawn restriction. It also validates block, item, and entity lookup direct,
  self, and fallback providers plus block cache updates in both integrated-world
  runs. It also creates a Fabric custom registry, observes its first entry, and
  verifies its attributes, loads and synchronizes a codec-backed Fabric dynamic
  registry from a datapack, observes its setup and entry callbacks, omits an
  empty optional registry from synchronization, and repeats all dynamic
  assertions after world reload before completing clean saves with no
  transformer exceptions.
  The client also proves all Content Registries v0 paths during Fabric
  initialization before creating, saving, and reloading the integrated world.
- A controlled Fabric command fixture registers and executes a real Brigadier
  command before and after a Forge 52.1.16 dedicated-server restart, with world
  save and clean shutdown.

## Intentionally gated

The adapter currently rejects Fabric API surfaces outside the implemented
modules, including Transfer API item-provided storage registration and fluid
storage, biome, convention-tag, trade-helper, and rendering surfaces, unknown custom
language adapters, Loader API calls outside the current
shim, and mods requiring unreviewed native-library behavior. Signed patch packs,
broad semantic graphical assertions, and catalog-wide compatibility measurement
are not yet implemented.
These gaps produce stable diagnostics instead of a JAR that is falsely labeled
compatible.

The controlled M2 fixture has passed on Minecraft 1.21.1 with Forge 52.1.0:
Fabric `preLaunch` and `main` ran on both sides, the correct Fabric `client` or
`server` entrypoint ran, a custom API entrypoint was discovered lazily from
`main` with its provider and definition intact, and `getRawGameVersion()`
returned `1.21.1`. The same fixture resolved Fabric-compatible `minecraft`,
`java`, and `fabricloader` containers with their expected metadata types,
versions, Minecraft-to-Java dependency, and real ordered Forge game-input root
paths. Fabric Loader's self-container metadata and icon resolved through the
Forge `union:` filesystem. A custom constructor member entrypoint also resolved
and instantiated through Forge's named module layers. The Kotlin adapter fixture
covers the eight entrypoint forms from Fabric Language Kotlin
`1.12.3+kotlin.2.0.21`: class, object class/function/property, companion
class/function/property, and top-level function. The fixture observed each side's
final Minecraft launch arguments. On the client, Fabric initialization
received the real `Minecraft` instance; on the dedicated server, the instance
was correctly null during initialization and became the constructed server at
Forge's about-to-start boundary. Both sides completed world save, shutdown, and
reload cycles. The server scenario repeated the complete cycle through two
Forge launches and emitted a passing structured report. This is
controlled-fixture evidence, not evidence that arbitrary Fabric mods are
compatible. Catalog-wide measurement remains pending the frozen 1,000-project
catalog. The real-mod probes above establish the M3/M4 reference gates on
macOS; they do not establish Windows/Linux parity or a 95% result.

## Build and use

```shell
./gradlew build
./gradlew :cli:installDist
./gradlew -p client-lab runClient

cli/build/install/cli/bin/cli inspect path/to/mod.jar --json
cli/build/install/cli/bin/cli prepare \
  --minecraft 1.21.1 \
  --host forge \
  --forge-version recommended \
  --mods path/to/mods \
  --output path/to/prepared

cli/build/install/cli/bin/cli verify \
  --instance path/to/installed-forge-server \
  --side server \
  --timeout-seconds 120 \
  --expect-marker LOADERBRIDGE_FIXTURE_MAIN_READY \
  --expect-marker LOADERBRIDGE_FIXTURE_SERVER_READY

CURSEFORGE_API_KEY=... cli/build/install/cli/bin/cli catalog freeze \
  --snapshot-id 2026-08 \
  --frozen-at 2026-08-01T00:00:00Z \
  --output catalog-2026-08.json

cli/build/install/cli/bin/cli resolve \
  --project modrinth:AABBCCDD \
  --output path/to/resolved-instance

cli/build/install/cli/bin/cli test \
  --scenario scenarios/controlled/server-lifecycle.yaml \
  --instance path/to/installed-forge-server \
  --artifacts build/scenario-artifacts \
  --json
```

`catalog freeze` queries both official repositories. The CurseForge key is read
only from the process environment, is sent only to CurseForge API metadata
endpoints, and is never written to snapshots or sent to artifact CDNs.
`resolve` installs the selected release and its recursively required
dependencies under `mods/`, retaining verified downloads in `.cache/` and
writing `bridge.repository.lock.json`.
`test` runs bounded lifecycle/save/reload behavior and writes a structured
`scenario-report.json` plus per-launch Forge transcripts and discovered logs.

For verification, install every generated artifact (including automatically
selected runtime libraries), `forge-runtime`,
`forge-transform-service`, and `fabric-loader-shim` in the Forge instance's
`mods` directory first. `verify` uses that instance's `run.sh` or `run.bat` and
automatically uses the same Java runtime as LoaderBridge.

Exit codes are `0` success, `2` invalid input, `3` unsupported capability or
dependency, `4` transformation failure, and `5` launch/verification failure.

The original mod JAR is never modified. Do not redistribute transformed
third-party artifacts unless their licenses permit it.
