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
