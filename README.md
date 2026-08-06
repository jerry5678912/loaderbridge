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
- Authenticated CurseForge and public Modrinth catalog providers with
  author-disabled artifact filtering, typed transient-network retries, bounded
  metadata timeouts, and deterministic four-worker page resolution. A live
  probe froze 1,000 unique installable Fabric 1.21.1 projects on 2026-08-06.
  Its companion lock contains 1,126 checksum-pinned artifacts and 1,444
  explicit declared-to-resolved required edges. A 29.3 MB normalized input
  capture reproduced both the snapshot and dependency lock byte-for-byte
  without credentials or live repository access, satisfying the M0
  reproducibility gate. Scheduled public snapshot publication remains separate.
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
  A controlled Fabric-only fixture also executes `Inject`, `Redirect`,
  `ModifyArg`, `ModifyArgs`, `ModifyVariable`, and `ModifyConstant`, plus a
  shadow, overwrite, accessor, and invoker through Forge on client and server.
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
  initial load, datapack reload, save, and process restart. Revision 2 also
  exposes Fabric built-in resource packs and data packs through Forge's native
  repository, preserves all three activation policies, synthesizes missing
  pack metadata, and rejects path and symbolic-link escapes.
- A separately versioned Resource Conditions API v1 bridge matching
  `fabric-resource-conditions-api-v1:4.3.0+8dc279b119`. It implements Fabric's
  public condition registry and custom codecs plus `true`, logical, loaded-mod,
  tag, feature, and registry predicates. JSON resources are filtered at the
  beginning of the apply phase, after current tags and enabled feature flags
  are captured. Automatic selection inspects the structured
  `fabric:load_conditions` key rather than filenames or mod IDs. Conditional
  `fabric:overlays` metadata is detected separately and currently stops with
  `LB-FAPI-004` instead of being silently misinterpreted.
- A separately versioned Loot API v3 bridge matching
  `fabric-loot-api-v3:1.0.3+3f89f5a519`. It injects Fabric's built-object
  builder extensions into vanilla loot builders and invokes `REPLACE`,
  `MODIFY`, and `ALL_LOADED` during Forge's native reloadable-registry load.
  Resource sources distinguish vanilla, mod, external datapack, and replaced
  tables. Bytecode inspection installs the bridge and its API-base/resource
  dependencies automatically.
- A separately versioned Recipe API v1 bridge matching
  `fabric-recipe-api-v1:5.0.16+2475392c19`. It implements Fabric custom
  ingredient registration, the five built-in ingredient serializers,
  `fabric:type` JSON codecs, Forge-native matching, and negotiated client
  synchronization with a vanilla matching-stack fallback for unsupported
  serializers. Inspection installs Recipe API and its networking, lifecycle,
  and API-base dependencies automatically.
- A separately versioned Entity Events v1 bridge matching
  `fabric-entity-events-v1:1.8.0+2b27e0a419`. Its complete pinned public API
  surface is selectable from metadata and bytecode, including nested callback
  descriptors. Forge events cover matching host semantics while early Mixins
  preserve Fabric timing for damage/death, combat kills, conversions, sleep,
  bed validity and occupation, wake positions, and elytra hooks.
- A separately versioned Data Attachment API v1 bridge matching
  `fabric-data-attachment-api-v1:1.4.7+5b36e0f719`. Its first M5 increment
  implements the public registry, type, predicate, and target contracts;
  identity-keyed storage on entities, block entities, levels, and chunks;
  codec-backed entity and block-entity persistence; dirty-state propagation;
  and Fabric copy rules for respawn, world changes, and mob conversion.
  Inspection selects its Entity Events, Object Builder, Networking, and API
  Base dependencies automatically. Server-level SavedData and chunk serializer
  persistence now survive process restarts. Revision 6 adds structured target
  addressing, codec-backed initial and mutation synchronization, tracking and
  chunk-watch delivery, predicate filtering, and client application; a
  graphical save/reload scenario proved the wire path in two client sessions.
  Revision 7 adds a configuration-phase request/response task that intersects
  the client and server attachment-type sets before any play packets are sent.
  Revision 8 proves correctly ordered level, player, entity, block-entity, and
  chunk delivery, including a persistent entity already loaded before the
  second client login. Revision 9 adds bounded deterministic packet partitioning,
  a 1 MiB payload ceiling, stable oversized-value diagnostics, Fabric's
  warn-or-strict unknown-target policy, and a generated-ProtoChunk transfer
  proof through the real world-generation conversion path. This completes the
  pinned Data Attachment API v1 module gate.
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
  Revision 5 transparently assigns phase-and-direction-specific Forge wire IDs,
  so one logical Fabric payload ID can be registered in both directions for
  play and configuration traffic. Remote channel queries translate those wire
  IDs back to the original Fabric IDs.
- A complete pinned Screen Handler API v1 bridge matching
  `fabric-screen-handler-api-v1:1.3.91+b559734419`. Content inspection selects
  it with Networking API and API Base automatically. Extended factories,
  typed opening-data codecs, custom client menu construction, replacement-menu
  close semantics, simple-factory unwrapping, and factory/type mismatch checks
  run through Forge's existing menu and payload infrastructure. Unknown menu
  IDs and missing client screens retain Fabric's warning-and-ignore behavior.
- A separately versioned Object Builder API v1 bridge matching
  `fabric-object-builder-api-v1:15.2.1+40875a9319`. It currently implements
  `FabricBlockEntityTypeBuilder`, the injected `FabricBlockEntityType` and
  vanilla builder interfaces, `FabricDefaultAttributeRegistry`, and the
  deprecated `FabricEntityTypeBuilder` base/living/mob contracts and the modern
  `FabricEntityType.Builder` interface injected into Minecraft's builder.
  Entity dimensions, tracking, velocity updates, feature requirements,
  attributes, Fabric's unnamed build behavior, and spawn restrictions are
  translated to Minecraft/Forge behavior. `BlockSetTypeBuilder` and
  `WoodTypeBuilder` also construct and register native behavior records with
  Fabric-compatible copying and defaults. Villager, wandering-trader, and
  rebalanced-pool registrations are translated through Forge's trade build
  events. Custom minecart comparator callbacks are stored by entity-type
  identity and invoked from powered detector rails before vanilla comparator
  fallback. Fabric common entrypoints run sequentially in resolved order
  inside Forge's registration window. Revision 8 passed fresh and saved-world
  dedicated-server gates on Forge 52.1.0 and 52.1.16, including a real
  detector rail and minecart returning the registered value `11`.
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
  `fabric-convention-tags-v2:2.12.0+c3656daa19`. Revision 2 exposes all nine
  pinned public classes and 494 block, item, fluid, entity-type, enchantment,
  structure, and biome keys; `TagUtil`; and Fabric's injected `TagKey`
  translation methods. The exact standardized `c:` data and translations merge
  with Forge's native common tags, including Fabric aliases Forge does not ship.
- A Tag API v1 bridge matching `fabric-tag-api-v1:1.3.0+1eb36c0719`.
  It exposes Fabric's injected `FabricTagFile` ABI and structurally translates
  the `fabric:remove` JSON member into Forge's native ordered tag-removal
  engine. Existing Forge `remove` entries are preserved and malformed fields
  remain errors rather than being silently accepted. A dedicated-server
  fixture retained stone, removed dirt, saved all dimensions, restarted, and
  repeated the assertion on Forge 52.1.16.
- A Fabric Dimensions v1 bridge matching
  `fabric-dimensions-v1:4.0.1+65213ef819`. It preserves Fabric's fail-soft
  world-data behavior when a custom dimension is removed. Preparation selects
  its isolated startup agent from inspected capabilities and writes the exact
  JVM option to `loaderbridge.launch.json`. Controlled worlds created, mutated,
  saved, reopened without their dimension datapack, saved again, and stopped
  cleanly on Forge 52.1.0 and 52.1.16.
- A server/shared Fabric Message API v1 bridge matching
  `fabric-message-api-v1:6.0.14+6ced4dd919`. Phased decorators and chat,
  game/system, and command allow/observe callbacks run at Minecraft's real
  broadcast boundaries. Fresh and saved-world gates passed on Forge 52.1.0 and
  52.1.16. Client send/receive callbacks remain an explicit M6 gap.
- A Fabric Crash Report Info v1 bridge matching
  `fabric-crash-report-info-v1:0.2.29+0af3f5a719`. It augments Minecraft's
  actual system report with sorted translated Fabric root and nested mod IDs,
  names, and versions. Metadata dependency inspection selects it automatically;
  fresh and saved-world dedicated-server gates passed on Forge 52.1.0 and
  52.1.16.
- A Biome API v1 bridge matching `fabric-biome-api-v1:13.0.31+d527f9fd19`.
  Fabric biome selectors plus placed-feature, configured-carver, and mob-spawn
  additions are translated into a registry-aware Forge biome modifier.
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
  Revision 8 adds Fabric's `SingleVariantItemStorage` and proves
  `ItemStorage.ITEM` provider registration with a component-backed portable
  storage, including rollback, commit, capacity, resource filtering, and
  container-item replacement. Revision 9 adds the first fluid-storage wave:
  Fabric fluid units and variants, transactional `SingleFluidStorage`, NBT
  persistence, sided block lookup, combined item providers, and vanilla bucket
  fill/drain. Both a graphical client and a dedicated server passed fresh-world,
  save, reload, and clean-stop gates on Forge 52.1.16. Revision 10 adds the
  remaining pinned server-safe public types: fluid attributes, player
  interaction utilities, transactional cauldrons, and vanilla water-potion
  containers. Revision 11 adds Fabric's built-in item providers for all
  shulker-box variants and bundles, transactional composter storage, stable
  vanilla inventory-provider lookup, and combined chest discovery. See
  [the controlled evidence](docs/controlled-fixtures/fabric-transfer-api-v1.md).
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
- Oxidized `1.8.4` is a pinned Fabric-only technology/content probe whose
  gameplay path currently passes but whose graphical gate is failed pending
  repair. Manual inspection found the kiln's placed model and inventory icon
  incorrect, invalidating the earlier object-identity model check.
  A generic Fabric-patched Minecraft ABI repair lets its SafLib dependency
  register the real `oxidized:copper_kiln` block entity on Forge. Registration,
  placement, clay-and-coal kiln processing into a brick, machine-output
  persistence, save, and reload pass; correct rendered assets do not yet pass.
  The current late-item model bridge is therefore not accepted as complete. A generic
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
  It also registers a Fabric `DEFAULT_ENABLED` built-in data pack, observes
  Forge discovering and selecting it automatically, reads its marker from the
  live server resource manager, and repeats the assertion after a complete
  process/world reload and in both graphical integrated-world sessions.
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
modules, including client fluid rendering, remaining trade-helper and broader
rendering surfaces, unknown custom language adapters,
Loader API calls outside the current
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
final Minecraft launch arguments. Its embedded Fabric child is recursively
extracted and transformed, loaded as an independent mod, linked back to the
parent through `getContainingMod()` / `getContainedMods()`, and invoked on both
sides. The parent also directly loaded a non-entrypoint class and resource found
only in that transformed child, proving shared game-layer classpath visibility
on both dedicated-server launches. That child depends on a `provides` alias
rather than the parent's primary ID: LoaderBridge preserves the alias for
Fabric lookup and dependency semantics,
while rewriting the generated Forge ordering edge to the canonical provider ID.
The alias path also passed both launches of the dedicated-server save/reload
scenario. On the client, Fabric initialization
received the real `Minecraft` instance; on the dedicated server, the instance
was correctly null during initialization and became the constructed server at
Forge's about-to-start boundary. Both sides completed world save, shutdown, and
reload cycles. The server scenario repeated the complete cycle through two
Forge launches and emitted a passing structured report. This is
controlled-fixture evidence, not evidence that arbitrary Fabric mods are
compatible. Catalog-wide measurement remains pending the frozen 1,000-project
catalog. The real-mod probes above establish the M3/M4 reference gates on
macOS; they do not establish Windows/Linux parity or a 95% result.

M2's acceptance gate was revalidated on 2026-08-04 after the source-level
Loader API audit. The dedicated-server scenario passed every Loader, Kotlin,
member-entrypoint, nested-classpath, alias, Mixin, save, reload, and clean-stop
step across two launches. The client lab reached the title screen, opened and
saved an integrated world, returned to the title screen, reloaded the world,
and stopped itself cleanly. This marks M2 complete for the controlled scaffold;
it is not a claim that arbitrary real mods or Fabric API modules work yet.

M3's controlled runtime-injector fixture passed on 2026-08-04. Both Forge
dedicated-server launches emitted `LOADERBRIDGE_FIXTURE_STANDARD_MIXINS_READY`
around a clean save/reload cycle, and the Forge client emitted the same marker
before title-screen, integrated-world save, reload, and clean stop. This proves
the listed standard injector families against a controlled official-namespace
target. The pinned Fabric Lithium 0.14.3 gate also passed client and dedicated
server launches on Forge 52.1.0 and 52.1.16 in the macOS laboratory, including
world creation, save, clean shutdown, and process/world reload. A separate
nine-JAR Lithium bundle, containing no other transformed mods or controlled
fixtures, repeated the Forge 52.1.16 client lifecycle successfully. This marks
M3 complete for the current macOS scaffold. Windows/Linux parity and broad
transformer-collision measurement remain later matrix work; this is not a 95%
catalog result.

M4's access-widener gate passed on 2026-08-04. Preprocessing now validates
official class, field, method, and descriptor targets against the intermediary
Minecraft structure before accepting a transformed JAR, with stable
`LB-AW-005` and `LB-AW-006` diagnostics for missing targets. The real nested
`fabric-transitive-access-wideners-v1` module from Fabric API 0.116.15 prepared
successfully and ran through the Forge 52.1.16 graphical Blockus world,
including model/resource readiness, placement, save, and reload. A freshly
translated FerriteCore 7.0.3 independently completed the Forge 52.1.16 client
world lifecycle and a two-process dedicated-server ready/save/reload/clean-stop
scenario. Together with the class, member, finality, inheritance, transitive,
and malformed-widener fixtures, this marks M4 complete for the current macOS
scaffold; cross-platform matrix parity remains a later gate.

M5 is in progress. Its Resource Conditions API v1 increment passed on
2026-08-04. The pinned Fabric-only Blockus 2.9.18 artifact automatically
selected the bridge from conditional recipe JSON, and Forge 52.1.16 filtered
recipes for the absent Modern Industrialization mod before serializer parsing.
The fresh graphical run reported zero such parse errors, loaded 4,622 recipes
on both initial load and reload, validated the real Blockus block and inventory
models, placed the block, saved the disposable world, reopened it, verified the
block and item persisted, and stopped cleanly. This closes one M5 module; it
does not complete M5 or establish the 60% catalog gate.

The Loot API v3 M5 increment passed on 2026-08-04. A translated controlled
Fabric fixture replaced vanilla cobblestone loot with a diamond pool, then
added an emerald through the modify event. Forge 52.1.16 produced both items in
an integrated-world loot execution, saved them in the player's inventory,
reopened the world, verified both persisted, and stopped cleanly. The dedicated
server independently invoked all three events during initial load and
`/reload`, reached ready, saved every dimension, and stopped cleanly. See
[the controlled evidence](docs/controlled-fixtures/fabric-loot-api-v3.md).

The Recipe API v1 M5 increment passed on 2026-08-04. A translated controlled
Fabric fixture registered its own count-sensitive ingredient serializer,
exercised all five built-in serializers, and loaded a `fabric:type` recipe that
matched cobblestone and assembled a diamond. The unchanged final bridge JAR's
graphical Forge 52.1.16 run negotiated all six serializers, transmitted the
custom ingredient natively, created and saved a world, reopened it, and stopped
cleanly. The tightened final fixture independently proved all five built-ins
and the recipe result on the dedicated server before and after `/reload`, then
flushed every dimension and stopped cleanly. See
[the controlled evidence](docs/controlled-fixtures/fabric-recipe-api-v1.md).

The Entity Events v1 M5 controlled module gate passed on 2026-08-05. Dedicated
Forge runs proved exact damage and fatal-death values, cancellation, actual
death and combat callbacks, mob conversion, world flush, clean stop, and a
second-process reload. The graphical run required all nine sleep callback
families plus custom elytra start/tick, mob and player cross-dimension changes,
and an actual alive respawn with matching copy/after-respawn values. Every
counter doubled after reopening the saved world; player leave fired on the
final disconnect and the process stopped cleanly. See
[the controlled evidence](docs/controlled-fixtures/fabric-entity-events-v1.md).

The Data Attachment API v1 M5 base increment passed on 2026-08-05. A translated
controlled Fabric fixture used actual Forge-transformed Minecraft entities,
block entities, levels, and chunks; proved default initialization and mutation,
entity and block-entity codec/NBT round trips, chunk dirtying, automatic bridge
dependency selection, world flush, and clean dedicated-server shutdown. This
is a base increment rather than the full module gate; see
[the controlled evidence](docs/controlled-fixtures/fabric-data-attachment-api-v1.md).
The follow-up persistence wave used a fresh world and two separate Forge server
processes to prove level value `41` and chunk value `43` survive disk save and
reload through SavedData, chunk serialization, and the ImposterProtoChunk
wrapper path.
Revision 6 then passed a graphical Forge 52.1.16 run: the client observed
server-level value `53` from join-time initial synchronization and player value
`59` from live mutation synchronization, both before and after saving and
reopening the world. A dedicated-server regression restored `41/43` and stopped
cleanly. Revision 7 added a blocking configuration task and negotiated all five
syncable fixture attachment types in each of the two graphical connection
sessions;
unit coverage also removes an unknown client-only ID from the accepted set.
Revision 8 moved entity initial synchronization to Fabric's packet-order
boundary after the spawn bundle. A fresh graphical run synchronized level `53`,
player `59`, entity `67`, block entity `71`, and chunk `73` in both sessions.
Only session 1 spawned the persistent entity, proving that the already-loaded
entity was synchronized on session 2 after save/reload. Revision 9 partitions
batches within the 1 MiB clientbound limit, rejects oversized single values as
`LB-ATTACH-005`, and reports missing client targets as `LB-ATTACH-006` under
Fabric's default-warning or strict-failure policy. A controlled world-generation
Mixin seeded value `79` on the genuine pre-conversion `ProtoChunk`; Fabric's
`CHUNK_GENERATE` callback then observed `79` on the resulting `LevelChunk`.
That closes the pinned Data Attachment API v1 module gate, but does not complete
M5 or establish its 60% catalog acceptance gate.

The Transfer API v1 revision 8 item-provider increment passed on 2026-08-05.
The translated lifecycle fixture registered a provider for a portable item,
resolved it through `ContainerItemContext.find(ItemStorage.ITEM)`, rolled back
an aborted component mutation, committed 700 units, extracted 200, rejected a
different resource, and observed the final 500-unit value and 1,000-unit
capacity. The same graphical Forge 52.1.16 process completed its bidirectional
networking, world save, reopen, and clean-stop gates. This closes the generic
item-provided single-variant storage increment, not the remaining fluid and
built-in-provider Transfer API surfaces or M5.

Transfer API v1 revision 9 passed later on 2026-08-05. The bridge normalizes a
flowing-water variant to still water, rolls back and commits fluid insertions
and extractions, reports the expected 54,000-droplet remainder, fills and drains
a vanilla bucket transactionally, resolves the tank through `FluidStorage.SIDED`,
and round-trips it through NBT. The Forge 52.1.16 graphical client proved this
before and after save/reload in one process; two dedicated-server launches
independently proved fresh-world and saved-world readiness plus clean shutdown.
This closes the initial fluid-storage foundation, not the remaining built-in
potion/cauldron, attribute/rendering, container-component, or bundle surfaces,
the whole Transfer module, or M5.

Transfer API v1 revision 10 passed on 2026-08-05. It adds all remaining public
server-safe types in the pinned module: cauldron registration/storage, validated
fluid attributes and sounds, and player-hand fluid interaction. It also bridges
vanilla glass-bottle/water-potion conversion. The graphical client proved
survival hand fill/drain in both sessions and cauldron rollback, committed level
changes, save, and reload. Two dedicated-server launches independently proved
fresh-world cauldron mutation, saved-world restoration, and clean shutdown.
Client-only fluid rendering and the remaining container-component/bundle
providers are still open, so this does not close the entire Transfer module or
M5.

Transfer API v1 revision 11 passed on 2026-08-05. All seventeen shulker-box
items now expose their 27-slot container component, bundles expose their native
weight-limited contents, and both update the containing item transactionally.
The fixture rolled back and committed shulker and bundle mutations, routed 70
diamonds across shulker slots, retained 65 after extraction, and retained 15 in
the bundle. `ItemStorage.SIDED` now also covers Fabric's transactional composter
top/bottom providers, stable `WorldlyContainerHolder` inventories, and combined
chests. Composter insert and bone-meal extraction each passed abort and commit
paths on both graphical world sessions and two dedicated-server processes.
Repeated preparation produced byte-identical bridge and transformed-fixture
JARs. This closes the pinned common/server Transfer API surface; client fluid
rendering remains an M6 concern, and M5's catalog gate remains open.

Fabric Biome API v1 revision 2 passed on 2026-08-05. In addition to placed
features, `BiomeModifications.addCarver` now resolves configured carvers through
the dynamic registry and `addSpawn` preserves Fabric's category, weight, and
group-size semantics while rejecting invalid MISC and unregistered entity
types. The translated lifecycle fixture added the Nether Cave carver and its
custom monster spawn to Plains, then observed both in the live biome on a
graphical world creation/reload cycle and two independent dedicated-server
processes. Both deterministic preparations produced byte-identical bridge and
fixture JARs. The general phased `BiomeModification` context, removals, climate,
effects, and specialized Nether/End helpers remain open, so this is not the
complete Biome module, M5, or the 60% catalog gate.

Fabric Biome API v1 revision 3 passed on 2026-08-05. The bridge now exposes the
general `BiomeModification` and nested context contracts, sorts registrations
by Fabric phase, identifier, and registration order, and maps those phases onto
Forge's native ADD, REMOVE, MODIFY, and AFTER_EVERYTHING passes. Generation
feature/carver additions and removals, spawn additions/removals/probability and
costs, all weather setters, and non-clearing effects setters operate on Forge's
live biome builders. The fixture removed Plains' vanilla Cave carver, changed
temperature to 0.42 and fog to `0x123456`, and installed a custom spawn cost;
the exact assertions passed during graphical create/reload and two dedicated
server processes. Effect clearing and spawn-cost clearing currently fail with
stable `LB-BIOME-004`/`005` diagnostics because Forge 52 exposes no safe public
removal hook. Nether/End biome-source helpers also remain open, so the module
and M5 catalog gates remain unclaimed. During context-sensitive rules,
`BiomeSelectionContext.getBiome()` still exposes the stable pre-modification
biome rather than a synthetic view of the in-progress Forge builder; selectors
and key/tag/dimension queries are supported, while current-state reads remain a
tracked parity gap.

Fabric Biome API v1 revision 4 passed on 2026-08-05 and supersedes revision
3's clearing and current-state limitations. Required bridge-owned Mixin
accessors clear every optional effects field and individual spawn costs before
the modified biome is built. `BiomeSelectionContext.getBiome()` now constructs
a synthetic biome from Forge's in-progress climate, effects, generation, and
spawn builders, so a later Fabric phase can observe an earlier phase's
temperature. The translated fixture set then cleared a foliage override and a
zombie spawn cost and observed the intermediate temperature. Exact assertions
passed during graphical create/reload and two independent dedicated-server
processes; repeated preparation produced byte-identical revision-4 bridge and
fixture JARs. Registry-entry current-state parity, configured-feature and
structure lookup parity, and specialized Nether/End helpers remain open, so
this still does not complete the Biome module, M5, or its 60% catalog gate.

Fabric Biome API v1 revision 5 passed on 2026-08-05. Selection contexts now
reverse-resolve registered configured features, placed features, and structures
by object identity, validate structures against their biome holder sets, and
query actual level-stem biome sources rather than approximating the three
vanilla dimensions with tags. Forge decodes biome modifiers before its level-
stem registry exists, so dimension access is intentionally deferred until
`ServerLifecycleHooks` applies modifiers with a complete server registry. The
same boundary also builds cached identity indexes after Forge has frozen and
rebound dynamic-registry holders, retaining Fabric's identity semantics without
rescanning entire registries for each selector call. The
controlled Plains rule proved both feature keys, positive and negative
structure validity, and Overworld-versus-Nether membership during graphical
create/reload and two independent dedicated-server processes. Repeated
preparation produced byte-identical revision-5 artifacts. Specialized
Nether/End source mutation helpers remain open, so the complete Biome module,
M5, and the 60% catalog gate remain unclaimed.

Fabric Biome API v1 revision 6 passed on 2026-08-05. The bridge now exposes
both pinned `NetherBiomes.addNetherBiome` overloads and
`canGenerateInNether`, retains deterministic registration order, and injects
the additions into Minecraft's native Nether multi-noise preset before biome
sources are constructed. The controlled Fabric fixture registered Plains at a
real climate parameter point and observed it through the live Nether level-
stem source during graphical create/save/reload and two independent dedicated-
server processes. Repeated preparation produced byte-identical revision-6
artifacts. `TheEndBiomes` remains open, so the complete Biome module, M5, and
the 60% catalog gate remain unclaimed.

Fabric Biome API v1 revision 7 passed on 2026-08-05. The bridge now exposes
all five pinned `TheEndBiomes` registration forms. Its registry-aware source
codec resolves every custom holder before End source construction, world-seeded
simplex noise selects weighted main-island, highlands, and small-island
replacements, and midlands/barrens select against the chosen parent highlands
biome. The controlled fixture observed Plains, Desert midlands, and Badlands
barrens through real End source queries in both graphical world sessions and
fresh plus saved-world dedicated-server processes. Repeated preparation
produced byte-identical revision-7 artifacts. This closes the pinned public
Biome API class surface, but remaining behavioral parity audits, M5, and its
60% catalog gate remain open.

Fabric Block API v1 revision 11 passed on 2026-08-05. The bridge matches pinned
`1.1.0+0bc3503219`, injects the exact Fabric interfaces into vanilla `Block`
and `BlockState`, delegates custom appearance through the block-state contract,
and exposes `fabric:can_climb_trapdoor_above`. Inspection selected the module
from bytecode references. Its controlled Fabric content mod registers a mimic
block and block item with a valid blockstate, world model, inventory model, and
translation; the clean-world Forge 52.1.16 client emitted both API/content
markers with no missing-model warning, saved and reopened the world, and
stopped cleanly. Two dedicated-server processes independently proved fresh
world creation, saved-world reload, and clean shutdown. This closes the pinned
Block API v1 module gate, not M5 or the 60% catalog acceptance gate.

Fabric Events Interaction v0 revision 12 passed on 2026-08-05 for the pinned
server/shared surface of `0.7.14+ba9dae0619`. Inspection automatically selected
the interaction, API-base, and lifecycle bridges from binary references. A
translated Fabric fixture exercised Minecraft's real attack, use, and block
destruction paths in a Forge 52.1.16 integrated world, proving ordered callback
results, cancellation, successful destruction, and exact callback counts. The
same client process saved and reopened its world; two dedicated-server
processes created and reloaded another world and stopped cleanly. Client pick,
client block-break, client pre-attack, fake-player, and pick-aware surfaces
remain explicitly unsupported rather than falsely advertised, so this closes
one server/shared increment, not the complete module, M5, or its 60% catalog
gate.

Fabric Events Interaction v0 revision 13 passed later on 2026-08-05. It adds
the pinned `FakePlayer` class, weak-value world/profile caching, default and
custom profiles, a no-output untracked connection, safe gameplay overrides,
and advancement-owner protection. Content inspection selects the interaction
bridge and its networking dependency. The controlled translated fixture proved
the FakePlayer contract during both halves of a graphical save/reload run and
during two fresh/reload dedicated-server processes. This closes the server
FakePlayer increment; client interaction callbacks and pick-aware contracts
remain for M6, and M5's catalog gate remains open.

Fabric Item API v1 revision 14 passed later on 2026-08-05 for the first common
slice of pinned `11.3.0+467044f319`. Inspection automatically selected the
Item API, API Base, and Lifecycle bridges from binary references. A translated
Fabric content fixture registered a modeled item and proved exact injected
item, stack, settings, component-builder, and tooltip contracts; custom
durability reduced five damage to two, equipment selection returned the head
slot, and stack-aware remainder and creator-namespace overrides dispatched.
The graphical Forge 52.1.16 run had no fixture model/texture warning, saved and
reopened its world, and observed the behavior on both integrated-server starts.
Dedicated-server processes loaded the same saved world and stopped with every
dimension saved. Repeated preparation produced byte-identical artifacts. At
that revision, default-component, enchanting, recipe-pipeline, tooltip, and
client-animation hooks remained open, so it was an Item API increment rather
than the complete module or M5.

Fabric Item API v1 revision 15 passed on 2026-08-05. It adds the exact
`DefaultItemComponentEvents` root and nested contracts, preserving callback
order plus item, collection, and predicate mutation forms. Forge 52 caches its
gathered default item components separately from Minecraft's backing field, so
the bridge updates both atomically while retaining Forge additions. A real
Fabric callback added a default glint component to the fixture content item.
Both integrated-server starts and two dedicated-server processes observed
`glint=true`; fresh worlds, save, graphical reload, process reload, and clean
shutdown all passed. Enchanting, recipe-pipeline, and client Item API hooks
remain open, so the complete module and M5 gates remain unclaimed.

Fabric Item API v1 revision 16 passed on 2026-08-05. Forge 52 already exposes
stack-sensitive remainder hooks to its crafting, furnace, and brewing code, so
the bridge now delegates those generic hooks to Fabric's
`FabricItem.getRecipeRemainder(stack)`. An identity-bound thread-local carries
the result across Forge's separate predicate and retrieval calls, ensuring mod
code runs once. The fixture's Fabric-only item returned a gold nugget through
a real `Recipe.getRemainingItems` call while a vanilla water bucket still
returned a bucket. Registered as fuel through Fabric Content Registries, the
same item powered a real furnace that produced stone and retained the gold
nugget. Fresh graphical world creation, same-process save/reload, two separate
dedicated-server processes, and deterministic preparation passed. A direct
custom brewing scenario, enchanting, and client Item API hooks remain open, so
the complete module and M5 gates remain unclaimed.

Fabric Content Registries revision 2 and Item API behavioral revision 17
passed on 2026-08-05. The bridge now exposes the exact
`FabricBrewingRecipeRegistryBuilder` contract and invokes its build event with
Forge's native `PotionBrewing.Builder`. Its injected builder methods add item
and potion mixes with Fabric's feature gating and bulk water/awkward semantics.
The translated Fabric-only content item served as a real brewing ingredient:
three water potions became awkward potions and the ingredient slot retained
the Fabric-defined gold nugget. The result repeated across a fresh graphical
world, same-process save/reload, and two dedicated-server processes. Repeated
preparation remained byte-identical. Enchanting and client Item API hooks
remain open, so the complete module and M5 gates remain unclaimed.

Fabric Item API revision 18 passed on 2026-08-05. The bridge now advertises
the exact `EnchantingContext`, `EnchantmentEvents`, nested callback, and
`EnchantmentSource` contracts. Tri-state allow decisions run in table, anvil,
command, and compatible random-loot checks; default table behavior still
preserves Forge item overrides. The dynamic-registry hook rebuilds each
enchantment through Fabric's modify event while preserving its definition,
exclusive set, special effects, and list effects, and classifies vanilla, mod,
and external-data-pack sources. The controlled Fabric item was accepted for
Sharpness through both PRIMARY table filtering and ACCEPTABLE `/enchant`
execution. Its callback added a real XP-repair effect to Sharpness, changing a
one-point calculation to four. A fresh graphical world, same-process reload,
two dedicated-server processes, clean saves, and deterministic preparation
passed. Client tooltip/animation hooks and special creator namespaces remain,
so the complete module and M5 gates remain unclaimed.

Fabric Item API revision 19 passed on 2026-08-05. `getCreatorNamespace()` now
matches Fabric's registry-aware attribution rules: ordinary stacks use their
item namespace, potion and tipped-arrow stacks use the potion holder namespace,
and enchanted books containing exactly one stored enchantment use that
enchantment holder namespace. The translated fixture registered its own potion
and data-driven enchantment, proved potion and tipped-arrow attribution, saw the
enchantment through the `MOD` modify-event source, and attributed the resulting
enchanted book to the fixture. Both dedicated-server processes and both halves
of the graphical world save/reload cycle emitted `creator=potion+book`; the
final five-JAR preparation was byte-identical. This largely closes the pinned
common/server Item API surface. Client tooltip and animation hooks remain for
M6, and M5's catalog gate remains open.

Fabric Screen Handler API v1 passed its controlled M5 module gate on
2026-08-06. A translated Fabric fixture registered an
`ExtendedScreenHandlerType`, opened it from a real server-player join event,
sent typed `RegistryFriendlyByteBuf` data, constructed the registered client
screen, and verified `label=loaderbridge-screen,value=37`. The exact final JAR
passed both halves of the graphical saved-world open/reopen cycle, clean saves,
and a dedicated-server saved-world start/save/stop on Forge 52.1.16. Two
independent preparations produced byte-identical artifact sets. This closes the
pinned Screen Handler module gate, not M5 or its 60% catalog acceptance gate.

The Loader API contract suite also covers Fabric Loader 0.16.14's extended
semantic-version rules for prereleases, wildcard ranges, comparator chains,
tilde ranges, caret ranges, trailing-zero equality, and the special
trailing-dash range boundary.
It resolves the official Apache-2.0 Fabric Loader 0.16.14 artifact as a
test-only reference and compares every public API type, superclass, interface,
field, constructor, and method descriptor against the packaged shim. Extra
forward-compatible methods are allowed, but a missing or linkage-incompatible
pinned symbol fails the build.
The runtime mapping resolver reports Forge's actual `official` target namespace,
maps intermediary symbols to Mojang names, and does not falsely advertise Yarn
`named` mappings. New transformed JARs embed an `intermediary -> official`
mapping header; older LoaderBridge JARs using the internal `named` label remain
readable for cache and instance compatibility.

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
  --side client \
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

CURSEFORGE_API_KEY=... cli/build/install/cli/bin/cli catalog lock \
  --snapshot catalog-2026-08.json \
  --output catalog-2026-08.dependencies.lock.json

cli/build/install/cli/bin/cli catalog reproduce \
  --capture catalog-2026-08.inputs.json \
  --output reproduced-catalog-2026-08.json

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
It validates every root's recursively required graph, excludes roots that
cannot be installed from declared metadata, and writes a companion dependency
lock by default. `catalog lock` rebuilds that graph from a bounded, validated
frozen snapshot without re-querying rankings. Catalog roots exclude alpha
releases; required dependencies may use compatible alpha builds or exact
Fabric pins, and a cross-loader pin falls back only to a Fabric build from the
same repository project. Every substitution is explicit in `resolvedEdges`.
The freeze also writes a deterministic `.inputs.json` capture containing the
normalized public results of every ranking, version-list, and pinned-version
lookup. It never stores request headers or credentials. `catalog reproduce`
uses only that bounded capture through fail-closed replay providers and cannot
download artifacts or fall back to the network.
`resolve` installs the selected release and its recursively required
dependencies under `mods/`, retaining verified downloads in `.cache/` and
writing `bridge.repository.lock.json`.
`prepare --side client|server` applies Fabric environment filtering before
dependency resolution and recursively omits incompatible nested mods. The side
is locked in `bridge.lock.json`. Reusing an output directory removes stale JARs
listed by its prior LoaderBridge lock, while never deleting unlisted files or
paths outside that directory.
Preparation also writes `loaderbridge.launch.json`. Launchers must apply every
listed JVM argument before starting Forge. This is mandatory when the selected
bridges need code before ModLauncher creates its module layers; for example,
Fabric Dimensions emits
`-javaagent:.loaderbridge/agents/dimensions-datafix-agent.jar`.
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
