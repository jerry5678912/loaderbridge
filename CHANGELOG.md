# Changelog

## 0.1.0-SNAPSHOT

- Added launcher-neutral adapter contracts and stable structured diagnostics.
- Added safe Fabric metadata/nested-JAR inspection and local dependency planning.
- Added deterministic preparation, hashing, locks, reports, ASM analysis, and a
  golden-tested TinyRemapper wrapper.
- Added CLI inspect/prepare/verify commands with ServiceLoader adapter discovery.
- Added Forge language/transformation service scaffolds and the initial Loader
  API shim.

All notable user-facing changes will be recorded here.

## Unreleased

### Added

- Added the first Fabric Transfer API v1 bridge slice with thread-confined
  outer/nested transactions, rollback and commit propagation, LIFO close and
  outer-close callbacks, snapshot participants, automatic bytecode selection,
  contract tests, and a real Forge client initialization fixture.
- Added generic Fabric `Storage` and `StorageView` contracts with immutable empty
  storage, capability flags, filtered iteration, version guards, automatic
  selection, and real-client transactional insert/extract rollback and commit.
- Added slotted and single-slot storage contracts, ordered combined storage,
  insertion/extraction restrictions, filtering wrappers, transfer variants,
  resource amounts, and transfer preconditions, with real-client multi-slot
  composition coverage through world creation, save, reload, and shutdown.
- Added Fabric `StorageUtil`, `SingleVariantStorage`, and `BlankVariantView`
  contracts for atomic moves, simulations, stacking, resource queries,
  comparator output, snapshots, capacity hints, and NBT codecs; a real client
  verifies utility movement through the complete world lifecycle.
- Added Fabric item variants, single-stack/item storage, and vanilla inventory
  wrappers with component-aware identity, sided slot access, transactional
  rollback/commit, capacity rules, and a real-client two-slot behavioral test.
- Moved the Transfer API bridge into Forge's transformed game layer and made
  Fabric language adapters resolve entrypoints through that layer, preventing
  duplicate Minecraft class identities when API bridges link game classes.

- Added the initial Fabric Registry Sync v0 bridge with custom static registry
  builders, MODDED/SYNCED attributes, registry-local entry/remap events,
  automatic module selection, and real Forge client save/reload coverage.
- Added codec-backed Fabric dynamic registry registration, datapack loading,
  Forge synchronization, automatic module selection, and initial/reloaded-world
  behavioral coverage in the graphical client laboratory.
- Added Fabric dynamic registry setup views and entry callbacks before datapack
  population, plus `SKIP_WHEN_EMPTY` filtering for registry and tag sync; both
  are verified during initial connection and world reload.
- Translated Forge native ID-mapping events into Fabric registry-local remap
  callbacks with immutable full old/new raw-ID and resource-ID views.

- Added the complete public Fabric API Lookup v1 surface with unique typed
  lookup IDs; block, item, and entity direct/self/fallback providers; live
  block caches; custom lookup/provider maps; automatic dependency selection;
  and real-client state-change/save/reload coverage.

- Expanded the Fabric Object Builder API v1 bridge with the deprecated entity
  builder's base, living, and mob specializations, including Fabric-compatible
  unnamed builds, attributes, tracking, and native spawn restrictions. A real
  Forge client with FerriteCore verifies registration, world save, shutdown,
  and reload.
- Added the modern `FabricEntityType.Builder` injected interface with no-ID
  builds, velocity updates, living attributes, and mob restrictions, covered by
  the same real-client save/reload cycle.
- Moved Fabric common entrypoints into a single resolved-order Forge common
  setup registration window, opening both Minecraft and Forge registry guards
  before direct Fabric registry writes and retaining final Forge freezing.
- Completed the Fabric lifecycle-events-v1 1.21.1 public surface with exact
  server chunk load, generation, full-status transition, and unload hooks plus
  a real Forge 52.1.16 force-load/save/restart behavioral scenario.
- Added the exact Fabric resource-loader-v0 1.21.1 public types and ordered,
  registry-aware Forge server-data reload listener registration.
- Added Fabric Game Rule API v1 boolean, bounded integer, double, enum, visitor,
  callback, custom-category, command, serialization, and persistence behavior.

- Initial LoaderBridge project scaffold.
- Added validated, launcher-neutral repository provider, artifact, dependency,
  hashing, and pagination contracts for compatibility catalog providers.
- Added a ServiceLoader-discovered Modrinth v2 provider with ranked Fabric
  search, version/dependency parsing, and checksum-addressed downloads.
- Added an authenticated CurseForge provider with Fabric 1.21.1 discovery,
  paginated file resolution, dependency mapping, and verified CDN downloads.
- Added deterministic catalog freezing with platform-local ranking, latest-release
  selection, hash/source deduplication, top-up enforcement, and canonical JSON.
- Added paginated multi-provider catalog collection and the credential-aware
  `catalog freeze` CLI command.
- Added immutable version-ID lookup plus recursive required-dependency
  resolution with deduplication, installation ordering, and cycle diagnostics.
- Added canonical repository dependency locks and a repository-qualified
  `resolve` CLI that installs verified root and dependency artifacts.
- Added versioned compatibility-scenario, bounded step, execution-context,
  plugin, structured result, and failure-phase contracts for the M1 laboratory.
- Added a strict, bounded YAML 1.2 scenario parser that rejects aliases,
  arbitrary object tags, duplicate keys, unknown fields, and oversized inputs.
- Added sequential scenario execution for lifecycle, log, command, save,
  reload, shutdown, and dynamically provided plugin actions.
- Added process-backed Forge scenario sessions with clean reloads, bounded
  console commands, transcripts, latest-log discovery, and crash collection.
- Added the machine-readable `test --scenario` command and an initial controlled
  server lifecycle/save/reload scenario.
- Added checksum-pinned disposable Forge server installation with empty-target
  protection, bounded execution, transcripts, and stable failure codes.
- Added client scenario execution through fixed `run-client.sh`/`run-client.bat`
  launchers without allowing scenario documents to supply process commands.
- Added ServiceLoader-discovered semantic assertions over an authenticated,
  size-bounded, loopback-only test probe; bearer tokens are read from files.
- Added a validation-gated suite of 25 unique controlled client/server scenarios
  spanning lifecycle, commands, registries, networking, world state, rendering,
  resources, configuration, saving, shutdown, and reload.
- Added three retries for failures explicitly classified as infrastructure while
  preserving immediate failures for bridge or mod behavioral mismatches.
- Added a standalone Forge 52.1.0 graphical client laboratory with a guarded
  development-mod layout and automated title, world, save, clean disconnect,
  reload, and shutdown readiness markers.
- Expanded the independently implemented Fabric Loader 0.16 API shim with
  entrypoint containers and aggregated failures, validated ObjectShare
  callbacks, sanitized launch arguments, runtime context, corrected Version
  package/signatures, mod origins, containment, and the public metadata types.
- Added Fabric-compatible default language-adapter handling for class,
  static-field, static-method, and instance-method entrypoint declarations with
  exact provider and definition attribution.
- Wired parsed Fabric metadata into runtime mod containers so aliases,
  environment scope, dependency kinds, and version matching remain available
  through `FabricLoader` after Forge construction.
- Added an independently implemented Kotlin language adapter for Kotlin object,
  class, property, and member-method JVM shapes, selected from structured
  entrypoint metadata with stable rejection of unknown adapters.
- Added bounded recursive nested-JAR materialization, transformation, output,
  provenance reporting, and content-hash deduplication; Kotlin and Loader API
  requirements no longer trigger obsolete blanket preparation gates.
- Preserved nested parent ID and sub-location in transformed manifests and
  exposed order-independent containing/contained mod relationships with
  Fabric-compatible nested origin behavior at runtime.
- Added deterministic Forge-safe host IDs, translated Fabric dependency-order
  edges, runtime side filtering, and Fabric-compatible entrypoint resolution
  exceptions.
- Added Fabric Loader 0.16 semantic/opaque version values, public predicate
  terms and operators, interval algebra, and runtime dependency requirements.
- Added Fabric Loader rich metadata contracts for people, contacts, licenses,
  icons, descriptions, and recursively typed custom values.
- Added public environment annotations, `preLaunch` entrypoint compatibility,
  client entrypoint verification, multi-root classpath lookup, and a
  self-contained Forge game-layer Loader API shim artifact.
- Added intermediary-to-runtime `MappingResolver` data composition, bounded
  embedding in transformed JARs, and class/field/method runtime lookups.
- Added Forge-valid generated `pack.mcmeta` resources while preserving author
  metadata, eliminating the invalid `ResourcePackInfo` loading warning.
- Versioned the Fabric adapter at 0.2.0 and included the adapter artifact hash
  in preparation cache keys and locks so implementation changes cannot reuse
  stale transformed JARs.
- Added duplicate nested mod ID/version diagnostics and deduplication independent
  of filenames or archive hashes.
- Extended the controlled server scenario and graphical client laboratory to
  prove Fabric `preLaunch`, `main`, sided entrypoints, world save, clean stop,
  and world reload on Forge 52.1.0.
- Began M3 by translating universal Fabric Mixin metadata into deterministic
  Forge-visible manifest registrations while continuing to use Forge's bundled
  Mixin 0.8.7 runtime.
- Added a controlled Fabric `@Inject` fixture targeting Minecraft's real
  `MinecraftServer.runServer` method and required its marker through dedicated
  server save/reload; the same injection passed both integrated-server launches.
- Added stable `LB-MIXIN-ENV-001` gating for environment-scoped configurations
  until side-safe generated wrapper configs are implemented.
- Attached TinyRemapper's Mixin extension to intermediary transformations and
  golden-tested remapping of hard Mixin targets plus string injection selectors
  into descriptor-qualified official runtime names.
- Replaced the temporary environment-scoped Mixin gate with deterministic
  generated client/server wrapper configs that preserve original resources and
  retain plugin/refmap fields.
- Proved side selection in real Forge runs: a server-scoped Fabric mixin applied
  in both dedicated-server launches and remained absent from the graphical
  client while its save/reload lifecycle still passed.
- Added bounded Tiny-v2 resource mapping plus Mixin refmap translation for
  owners, methods, fields, and descriptors in default and contextual maps.
- Generated dual refmap lookup keys for original Fabric selectors and
  TinyRemapper's runtime selectors, with stable malformed-resource and
  translated-key collision diagnostics; original refmaps remain untouched.
- Expanded the Mixin remapping golden fixture across `@Inject`, `@Redirect`,
  `@ModifyArg`, `@ModifyArgs`, `@ModifyVariable`, `@ModifyConstant`, nested
  `@At` targets, `@Accessor`, `@Invoker`, `@Shadow`, and `@Overwrite`.
- Added a controlled standard Mixin config plugin and required its real callback
  marker across both Forge dedicated-server launches, save, and reload.
- Added annotation-aware MixinExtras capability detection and automatic output
  of the unmodified official Forge 0.5.4 game-library artifact, pinned by URL
  and SHA-256 in the compatibility report and bridge lock.
- Proved `@ModifyReturnValue` behavior on a real Minecraft server method across
  both Forge launches, save, reload, and clean shutdown.
- Replaced all-or-nothing namespace detection with a tested 95% dominance
  threshold and downgraded undeclared Fabric API references to a structured
  warning while keeping declared Fabric API dependencies as hard gates.
