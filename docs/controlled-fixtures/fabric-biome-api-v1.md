# Fabric Biome API v1 controlled fixture

This fixture tests LoaderBridge revisions 2 through 5 of pinned
`fabric-biome-api-v1:13.0.31+d527f9fd19` on Minecraft 1.21.1 and Forge
52.1.16. The ordinary `prepare` command inspects bytecode and automatically
selects the Biome bridge and its dependencies without relying on artifact
filenames or mod IDs.

The translated Fabric lifecycle fixture registers a custom monster entity and
uses the public Fabric contracts to add two entries to Plains:

- `Carvers.NETHER_CAVE` in the AIR carving step;
- the custom monster with weight 3 and a group range of 1 to 2.

The runtime assertion reads the live Plains biome after server startup and
requires both additions. It emits:

`LOADERBRIDGE_FABRIC_BIOME_ADDITIONS_READY spawn=1,carver=nether`

The marker appeared during both halves of a graphical world creation, save,
and reload scenario. It also appeared in two independent dedicated-server
processes: the first generated a new world and the second reloaded it. Every
process saved the overworld, Nether, and End and stopped cleanly.

Revision 3 adds a general phased modification registered through
`BiomeModifications.create`. Its REMOVALS phase deletes Plains' vanilla Cave
carver. Its POST_PROCESSING phase receives both selection and modification
contexts, verifies the selected biome key, disables precipitation, sets
temperature to 0.42 and downfall to 0.21, changes fog to `0x123456`, changes
creature spawn probability to 0.123, and installs a spawn cost for the fixture
entity. The live biome must expose every change and emits:

`LOADERBRIDGE_FABRIC_BIOME_PHASED_READY weather=0.42,fog=123456,cave_removed=true,cost=true`

That marker also passed both graphical sessions and both dedicated-server
processes. Automatic preparation initially rejected direct nested-context
references until the module descriptor advertised all four public nested
interfaces; the corrected planner then selected revision 3 without filename or
mod-ID hints.

Repeated preparation produced byte-identical artifacts:

- bridge SHA-256: `409a9e2a514bed848002ea7600903a1b2f9610282539c0d72f11da56f2eec0d3`
- transformed fixture SHA-256: `192729144903480a733e025bf9469935e52218af69ba77b12f42944d7a28010b`

Revision 3 deterministic artifacts:

- bridge SHA-256: `eebe4eb2cf1d9a7bb0b587b4a554b63d83630e948d6a216c37ea71b1b12af483`
- transformed fixture SHA-256: `98d168115b6571fea2244ee6b21dd2f60b0dad0d1f4c495d988dbffc5c98b686`

Revision 4 replaces revision 3's clearing diagnostics with required early
Mixin accessors. The ADDITIONS phase sets temperature to 0.31, installs a
foliage override, and installs a zombie spawn cost. POST_PROCESSING observes
the 0.31 temperature through `BiomeSelectionContext.getBiome()`, then clears
the foliage override and zombie cost before applying the final state. The live
biome assertions emit:

`LOADERBRIDGE_FABRIC_BIOME_CLEARING_READY effect=true,cost=true,current_state=0.31`

That marker passed both graphical sessions and both dedicated-server
processes. Two fresh preparations selected revision 4 automatically and
produced byte-identical artifacts:

- bridge SHA-256: `060d3ecba9fa1b652144d3a039a2c7b5a61f79f1c3403764d1f1b15c1269b545`
- transformed fixture SHA-256: `2bdb6039dbdc9745e92026c54d2be240bda3be7d5a9b2e018aae5fba78bfb59f`

Revision 5 replaces the remaining placeholder selection queries with dynamic
registry behavior matching Fabric's pinned implementation. During Plains
POST_PROCESSING the fixture reverse-resolves `PATCH_GRASS` and
`PATCH_GRASS_PLAIN`, accepts the Plains village but rejects the Nether fortress,
and confirms that Plains belongs to the Overworld source but not the Nether
source. It emits:

`LOADERBRIDGE_FABRIC_BIOME_REGISTRY_SELECTION_READY feature=true,placed=true,structure=true,dimension=true`

Forge's biome-modifier codec is decoded before the level-stem registry exists;
the first live attempt intentionally exposed that ordering mismatch as
`LB-BIOME-001: dimension registry is unavailable`. The corrected bridge keeps
feature and structure lookups from decode-time registry owners but defers
dimension lookup until Forge applies modifiers through its active server.
Because Forge also rebinds dynamic-registry holder values during freeze, the
bridge builds its object-identity indexes lazily at that same post-freeze
boundary. The
marker then passed graphical create/save/reload and fresh plus saved-world
dedicated-server processes. Repeated preparation produced byte-identical
artifacts:

- bridge SHA-256: `0b0917d2c0018e81c8006e1b58a61840a2fe50540cc7206f0cd0c3e219fb5cdc`
- transformed fixture SHA-256: `b69d7d75038585ea3ec3bbd6800e6c92be6074f131cfa273eb6df81baa8ee2bf`

The bridge emits `LB-BIOME-003` when a configured-carver target cannot be
resolved. Revision 4 covers general ordered phases, current-state biome reads,
weather and effects mutation including clears, generation additions/removals,
and spawn additions/removals/probability/cost installation and clearing.
`getBiomeRegistryEntry().value()` still refers to the registry's original
biome while a rule is executing. Specialized Nether/End source mutation
helpers also remain open. This does not complete the Biome API module, M5, or
the roadmap's 60% catalog gate.
