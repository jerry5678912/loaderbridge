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

- Hardened real catalog collection across Modrinth and CurseForge: author-
  disabled CurseForge downloads are skipped and topped up, transient request
  and response failures are typed and retried exactly three times, metadata
  requests have a 30-second bound, and four-worker page resolution preserves
  repository rank order. A live run froze 1,000 unique Fabric 1.21.1 projects.
- Added catalog-wide recursive required-dependency locking, snapshot SHA-256
  binding, dependency-first artifact order, lookup caching, duplicate-edge
  removal, and explicit declared-to-resolved edges. Installability validation
  now excludes broken roots and tops up rankings. Required alpha libraries and
  exact Fabric pins are supported; cross-loader pins may substitute only a
  compatible Fabric artifact from the same project. A live gate locked 1,000
  roots into 1,126 artifacts and 1,444 unique edges.
- Added the complete pinned Fabric Screen Handler API v1 contract with typed
  extended opening data, client screen construction, replacement-menu close
  semantics, simple-factory unwrapping, mismatch validation, and Fabric-style
  warning behavior for unknown client menu types. Bytecode inspection installs
  its Networking API and API Base dependencies automatically. A translated
  fixture passed graphical open/save/reopen and dedicated-server save/reload
  gates on Forge 52.1.16 with deterministic transformed artifacts.
- Completed the pinned Fabric Convention Tags v2 public contract: all nine
  classes, 494 tag keys, `TagUtil`, and `FabricTagKey` translation/name methods
  injected into Minecraft's real `TagKey`. The module carries the exact
  Apache-licensed `c:` data and translations so Fabric-only aliases merge with
  Forge's common tags. Real block, item, fluid, entity, and plural-alias
  membership passed graphical and two-process dedicated-server save/reload.
- Added Fabric Resource Loader v0 built-in resource-pack and data-pack
  registration through Forge's native pack repository. `NORMAL`,
  `DEFAULT_ENABLED`, and `ALWAYS_ENABLED` preserve selection semantics, packs
  without `pack.mcmeta` receive Fabric-compatible metadata, and canonical path
  checks reject traversal and symbolic-link escapes. A translated fixture's
  default-enabled data pack loaded automatically in graphical and two-process
  dedicated-server save/reload gates.
- Added Fabric `TheEndBiomes` with world-seeded weighted replacements for main
  islands, highlands, and small islands plus parent-aware midlands and barrens.
  A registry-aware End source codec includes custom holders before construction,
  and cached world-generation samplers retain the seed used by Fabric's picker.
  Deep source-selection assertions passed graphical and two-process dedicated-
  server save/reload gates.
- Added the pinned Fabric `NetherBiomes` contract and early Mixin integration
  with Minecraft's native Nether multi-noise preset. Registered biome/noise
  points now participate in actual Nether biome-source construction and
  `canGenerateInNether` queries. A translated fixture added Plains, observed it
  in the live Nether source, and passed graphical plus two-process dedicated-
  server save/reload and deterministic preparation gates.
- Completed Fabric Biome selection's registry-backed queries for configured and
  placed features, structure validity/key lookup, and dimension biome-source
  membership. Dimension resolution is deferred until Forge applies modifiers,
  avoiding its earlier biome-modifier decode phase; reverse identity indexes
  are likewise built after Forge freezes and rebinds dynamic-registry holders.
  Exact Plains assertions passed graphical create/reload and two-process
  dedicated-server gates.
- Completed the mutable-context portion of Fabric Biome API's general
  modification contract. Bridge-owned Mixin accessors now support clearing all
  optional biome effects and spawn costs, while selection callbacks receive a
  synthetic biome assembled from Forge's in-progress builder. A translated
  fixture observed an earlier phase's temperature, cleared a foliage override
  and vanilla zombie cost, and passed graphical plus two-process dedicated-
  server save/reload and deterministic preparation gates.
- Added Fabric Biome API's general `BiomeModification`, four ordered phases,
  and weather, effects, generation, and spawn contexts. Fabric phase and
  identifier ordering maps deterministically onto Forge's native biome
  modifier lifecycle; a translated fixture removed a vanilla carver and
  changed live climate, fog, spawn probability, and spawn cost across graphical
  and two-process dedicated-server reload gates.
- Expanded Fabric Biome API v1 with the standard `addCarver` and `addSpawn`
  convenience contracts. Configured carvers are resolved through Forge's
  registry-aware biome-modifier codec with stable missing-target diagnostics,
  while spawn entries retain Fabric's category, weight, and group bounds. A
  translated fixture proved both live Plains mutations through graphical
  world/save/reload and two dedicated-server processes on Forge 52.1.16.
- Added Fabric Transfer API's built-in item providers for every shulker-box
  variant and bundles, plus transactional composter insertion and bone-meal
  extraction, stable `WorldlyContainerHolder` discovery, and combined chest
  fallback. Aborted and committed component mutations, 27-slot routing, bundle
  weight limits, composter side rules, graphical save/reload, and two dedicated
  server processes now pass against Forge 52.1.16.
- Added Fabric Item API creator attribution for normal items, potion and tipped
  arrow stacks, and enchanted books containing exactly one stored enchantment.
  Attribution follows registry holder namespaces rather than artifact names: a
  translated fixture proved a mod-owned potion on both item forms and a
  mod-owned data-driven enchantment classified through the `MOD` source path.
  The result passed repeated graphical and dedicated-server world reloads and
  deterministic preparation.
- Added the pinned Fabric Item API enchanting contracts. Ordered tri-state
  decisions now cover enchanting-table, anvil, command, and compatible random
  loot checks, while data-driven enchantments are rebuilt through Fabric's
  modify event with their original definition, exclusivity, and effects
  preserved. A translated content item received Sharpness through real table
  filtering and `/enchant` paths, and a Fabric mutation changed Sharpness's
  live XP-repair result from one to four across graphical and two-process
  dedicated-server save/reload gates.
- Added Fabric's `FabricBrewingRecipeRegistryBuilder` contract and routes its
  build event through Forge's native brewing builder. Item and potion recipes,
  feature gating, and bulk water/awkward registrations use native Forge 52
  recipe entries. A translated Fabric content item brewed water into awkward
  potions in a real brewing stand, retained its Fabric-defined gold-nugget
  remainder, and passed graphical plus two-process dedicated-server
  save/reload gates.
- Added stack-aware Fabric recipe remainders to Forge's crafting and furnace
  pipelines. The bridge delegates Forge's stack hooks to `FabricItem` and
  carries one identity-bound result between Forge's separate predicate and
  retrieval calls, preventing duplicate mod callback execution. A translated
  Fabric-only item passed real recipe, vanilla-bucket control, furnace,
  graphical save/reload, and two-process dedicated-server tests.
- Added Fabric Item API v1 default component events with the exact callback and
  nested context contracts. Item, collection, and predicate mutations preserve
  Forge-gathered components while updating both Minecraft's backing map and
  Forge's component cache. A real content item acquired a default glint and
  passed graphical and two-process dedicated-server save/reload gates.
- Added the first common Fabric Item API v1 bridge slice matching
  `11.3.0+467044f319`. It injects Fabric item, stack, settings, component-map,
  tooltip, custom-damage, and equipment-slot contracts into Minecraft's real
  classes and is selected from inspected references. A translated content
  fixture registered a modeled item, proved custom durability and slot behavior
  on both sides, passed graphical save/reload plus dedicated-server process
  reload on Forge 52.1.16, and produced byte-identical repeated preparation.
  Unimplemented Item API surfaces remain unadvertised.
- Added the pinned Fabric Interaction Events `FakePlayer` surface with
  world/profile caching, default profile compatibility, a no-output untracked
  connection, gameplay-safe overrides, and advancement-owner protection. The
  translated fixture proved it on both halves of an integrated-world
  save/reload and two dedicated-server launches on Forge 52.1.16.
- Added the server/shared Fabric Events Interaction v0 bridge matching
  `0.7.14+ba9dae0619`. It translates ordered Fabric callbacks for block/entity
  attack, block/entity/item use, and before/after/canceled block breaking onto
  Forge and an early block-destruction Mixin. A controlled translated fixture
  passed exact behavioral counts, cancellation, successful destruction,
  integrated-world save/reload, and two-launch dedicated-server reload on Forge
  52.1.16. Unimplemented client and fake-player surfaces remain detectable
  preparation failures rather than advertised compatibility.
- Added the Fabric Block API v1 bridge matching `1.1.0+0bc3503219`. It injects
  Fabric's exact block and block-state appearance contracts into every vanilla
  target, preserves custom appearance delegation, exposes the functionality
  tag, and is selected from bytecode references. A controlled Fabric content
  mod registers a block and block item with working blockstate, world model,
  inventory model, and translation resources. It passed a fresh graphical
  world, save/reload, and two-launch dedicated-server reload on Forge 52.1.16.
- Added the Fabric Loot API v3 bridge matching `1.0.3+3f89f5a519`. It exposes
  the complete public event/source/builder surface, injects built-object
  builder operations, identifies vanilla/mod/datapack sources, and runs
  replace, modify, and all-loaded callbacks inside Forge's reloadable loot
  registry pipeline. A controlled Fabric fixture automatically selects its
  API-base and resource-loader dependencies, replaces cobblestone loot with a
  diamond, adds an emerald, executes both drops, persists them across an
  integrated-world save/reload, and repeats all event hooks across dedicated
  server startup and `/reload`.
- Added the Fabric Resource Conditions API v1 bridge matching
  `4.3.0+8dc279b119`. It implements the public condition registry and codecs,
  all nine standard condition types, current feature and tag capture, registry
  lookups, custom condition types, and apply-phase JSON filtering. Inspection
  selects it from the structured `fabric:load_conditions` content rather than
  filenames or mod IDs; conditional pack overlays stop at `LB-FAPI-004`
  until their separate pack-selection hook exists. The pinned Blockus client now
  filters its absent Modern Industrialization recipes before vanilla parsing
  and passes models, placement, save, world reload, and persistence on Forge
  52.1.16.
- Added preprocessing validation for remapped access-widener class, field, and
  method targets. Official rules are checked against the intermediary
  Minecraft structure without defining classes, including reverse member and
  descriptor mapping; missing targets fail as `LB-AW-005` or `LB-AW-006`. The
  Fabric-to-Forge adapter is now version 0.5.0 so locks and caches record this
  M4 behavior boundary.
- Added a controlled Fabric-only runtime fixture for all classic Mixin injector
  families plus shadow, overwrite, accessor, and invoker behavior. It passes
  Forge client and two-launch dedicated-server world save/reload scenarios.
- Added bytecode-level detection for Fabric Mixin local-capture semantics that
  differ from Forge's stock Mixin runtime. Modern Fabric Loader constraints
  remain supported; possible pre-0.12 behavior now stops at `LB-MIXIN-017`
  instead of failing unpredictably during transformation.
- Added side-aware `LB-MIXIN-002` preflight diagnostics for duplicate active
  Fabric Mixin configuration names, matching Fabric bootstrap behavior before
  LoaderBridge generates namespaced Forge wrappers.
- Batched Fabric `preLaunch`, `main`, `client`, and `server` entrypoints in
  canonical mod-ID order, independent of Forge container discovery order.
  Dedicated-server entrypoints now use the same once-only global coordination
  as the other Fabric stages.
- Replaced iterative nested-version choice with a deterministic bounded
  backtracking selector. It now chooses versions whose own hard dependencies,
  host-version predicates, aliases, identity claims, and `breaks` constraints
  form a consistent combination; search exhaustion reports `LB-NESTED-008`.
- Preserved nested parent-candidate links through hash deduplication. Children
  of unselected parent variants are no longer transformed, shared children use
  a selected containing parent, and incompatible optional nested mods may be
  omitted like Fabric's `IF_POSSIBLE` candidates.
- Rejected Fabric metadata that redefines Loader's reserved `default` language
  adapter or duplicates a language-adapter key across selected mods, using the
  stable `LB-LANG-002` planning diagnostic.
- Planned capabilities from every selected root and nested Fabric candidate,
  including bytecode references and language-adapter metadata. Compatibility
  reports now expose nested Fabric API, Loader API, MixinExtras, native-library,
  namespace, and unknown-adapter requirements before transformation.
- Added Fabric-style nested candidate selection: mandatory roots outrank nested
  copies, compatible higher versions win, selected dependencies are iterated,
  identical hashes are deduplicated, and a later root occurrence promotes a
  previously nested artifact. Duplicate mandatory roots remain an error.
- Ignored semantic `+build` metadata in preprocessing version comparison, as
  required by Fabric extended-semver dependency and priority behavior.
- Retained Fabric metadata author, contributor, person-contact, and mod-contact
  objects across repeated getters, matching Fabric's immutable parsed views.
- Parsed Fabric metadata custom values once and now return the same immutable,
  declaration-ordered map and value nodes across repeated API access.
- Matched Fabric `VersionInterval` cross-implementation equality, hashing,
  rendering, unbounded-end normalization, plain-version intersection/union
  rules, and overlap deduplication.
- Made Fabric Loader's config-directory accessor create the directory on first
  access and preserve its upstream failure wrapping for immediate mod writes.
- Matched Fabric mod-container identity semantics and the public diagnostic
  strings for containers, path origins, and recursively nested origins.
- Matched Fabric's cross-implementation `ModDependency` equality, hashing, and
  diagnostic string representation using parsed version predicates.
- Matched Fabric semantic-version equality across omitted trailing zero
  components, with consistent hashing and equivalent predicate deduplication.
- Matched Fabric Loader's immutable `CustomValue` tree behavior: stable nested
  value identity, ordered iteration, `Double` numbers, and precise
  `ClassCastException` failures for every invalid type conversion.
- Added a controlled nested-mod classpath probe proving that a parent Fabric mod
  can directly load a non-entrypoint class and resource that exist only in its
  transformed nested child JAR across both Forge server launches and world reload.
- Hardened untrusted `fabric.mod.json` parsing with Fabric-compatible container
  and scalar type checks for entrypoints, nested JARs, mixins, dependencies,
  language adapters, people, contacts, icons, and custom data. Malformed input
  now produces controlled inspection diagnostics instead of Gson cast failures.
- Exposed Fabric mod containers and entrypoints in the canonical mod-ID order
  produced by Fabric Loader 0.16.14, independent of Forge JAR discovery order,
  while retaining declaration order within each mod and alias lookup behavior.
- Mirrored Fabric Loader's published self-container description, author,
  contact, license, and icon metadata, and exposed the shim's real resource root
  so `ModContainer.findPath` works on Forge `union:` filesystems.
- Added Fabric default-language-adapter constructor entrypoints (`Class::<init>`)
  and reflection-backed typed member handles that remain usable across Forge's
  named game/plugin module layers.
- Exposed Forge's ordered Minecraft launch input paths as the Fabric builtin
  `minecraft` container roots, including multi-path client/common layouts,
  instead of using the unrelated game directory placeholder.
- Captured the exact final client/server Minecraft argument arrays through a
  structural ModLauncher transformer, preserving Fabric's sensitive-argument
  sanitization contract without private reflection. Published the real client
  instance before Fabric initialization and the real dedicated-server instance
  after construction, matching Fabric's sided lifecycle timing in controlled
  graphical and two-launch server scenarios.
- Added Fabric-visible `minecraft`, `java`, and `fabricloader` runtime
  containers with target-compatible metadata types and versions, Minecraft's
  Java 21 dependency, stable registration, and one shared Fabric Loader
  compatibility version for preprocessing and runtime. The controlled Forge
  server and graphical client fixtures validate these containers before world
  save and reload.
- Deferred Fabric `preLaunch` callbacks until Forge's construct event, after all
  active mod containers have installed their entrypoint definitions. The
  controlled fixture discovers a custom API entrypoint from `preLaunch` on
  client and dedicated server without moving registry-dependent `main`
  callbacks back before Forge registry population.
- Added Fabric Loader's raw game-version contract and source it from the
  transformed artifact's locked Minecraft version at Forge runtime.
- Added arbitrary Fabric entrypoint keys with lazy language-adapter resolution,
  one cached instance per requested contract type, provider/definition
  containers, and aggregated resolution failures. The transformed controlled
  fixture discovers a custom API entrypoint during `main` on both dedicated and
  graphical Forge runs, then passes save and reload.
- Added opt-in CLI exception detail through `-Dloaderbridge.debug=true` while
  retaining concise default transformation errors.
- Added generic recipe-book category discovery for Fabric cooking recipe types.
  LoaderBridge snapshots actual recipe-type registry additions around Fabric
  entrypoints and maps `AbstractCookingRecipe` instances to Forge's vanilla
  food, block, or miscellaneous furnace categories without mod-ID matching.
- Moved client Fabric initialization to Forge's recipe-book registration event,
  with common setup retained as the fallback. This keeps initialization after
  registry population while allowing custom cooking categories to be present
  before Forge freezes its recipe-book lookup table.
- Added a generic late-Fabric-item model bridge based on Forge's
  `RegisterAdditional`, `ModifyBakingResult`, and `BakingCompleted` model
  events. It snapshots actual registry additions, loads `models/item/`
  resources, aliases their logical inventory IDs, and updates the item
  renderer cache without filename or mod-ID matching.
- Added graphical assertions that reject Minecraft's missing block model,
  missing inventory model, and missing textures. The official Fabric-only
  Oxidized 1.8.4 probe now passes both model assertions, real kiln processing,
  save, and reload.
- Added a version-scoped, bytecode-shape repair for Fabric's no-argument
  `BlockEntityType.Builder.build()` ABI, supplying Forge 52.1.x's data-fixer
  argument. This moves the Fabric-only Oxidized 1.8.4 probe from construction
  failure to real copper-kiln registration, placement, recipe processing,
  output persistence, save, and reload.
- Extended the graphical client laboratory with a registry-driven three-slot
  machine scenario. It can insert configured input and fuel, wait for real
  server-tick output, and verify that output after world reload without linking
  the probe to a tested mod's classes.
- Added a same-repository-project catalog eligibility gate that queries the
  target loader and excludes projects with a native non-alpha Forge 1.21.1
  release; both Modrinth and CurseForge providers support this check. A frozen
  score still requires cross-site provenance review before calling a project
  Fabric-only.
- Added initial Convention Tags v2, Biome API v1, Block Render Layer v1, and
  Rendering v1 bridges with bytecode-driven automatic installation.
- Added model-layer, renderer, and color-provider compatibility plus generic
  registry-alias and Terraform boat-renderer bytecode repairs required by the
  first Fabric-only content probe.
- Passed the pinned Blockus `2.9.18+1.21.1` deep graphical probe on Forge
  52.1.0: real block/item registration, placement, inventory insertion, save,
  reload, persistence verification, and clean stop.
- Added Fabric `TradeOfferHelper` villager and wandering-trader registration
  over Forge trade events; the graphical fixture observes its callback on both
  initial world creation and reload.
- Expanded Object Builder API v1 with Fabric-compatible block-set and wood-type
  builders, including copy/build/register behavior and real-client native
  record registration.
- Added Fabric Item Group API v1 custom-tab builders and keyed/global entry
  callbacks with visibility and ordering behavior, automatic installation, and
  a real-client custom-tab rebuild on initial world join and reload.
- Added a generic Fabric Content Registries v0 bridge for fuel, composting,
  flammability, shovel flattening, axe stripping, oxidation, and waxing, with
  bytecode-driven installation and a real Forge client behavioral fixture.
- Added the first Fabric-only content-mod deep scenario for Blockus, requiring
  real block/item registration, placement, rendering, save, and reload rather
  than treating startup as compatibility.
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
- Added `ItemStorage.SIDED` with automatic lookup-module dependency selection
  and vanilla container fallback wrapping; the graphical client discovers a
  chest, inserts transactionally, saves, and repeats the lookup after reload.
- Added Fabric player inventory storage and container-item contexts with native
  hand-first stacking, transactional cursor and inventory mutation, deferred
  overflow drops, creative/simulation behavior, and graphical join/reload
  coverage.

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
