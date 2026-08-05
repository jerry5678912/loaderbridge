# Fabric Biome API v1 controlled fixture

This fixture tests LoaderBridge revisions 2 and 3 of pinned
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

The bridge emits `LB-BIOME-003` when a configured-carver target cannot be
resolved. Revision 3 covers general ordered phases, weather mutation,
non-clearing effects mutation, generation additions/removals, and spawn
additions/removals/probability/cost installation. Effect clearing and spawn-cost
clearing emit stable `LB-BIOME-004` and `LB-BIOME-005` diagnostics until an
early-transform accessor can implement them safely. Specialized Nether/End
helpers also remain open. Context-sensitive callbacks receive both public
contexts, but `BiomeSelectionContext.getBiome()` currently exposes the stable
pre-modification biome rather than a synthetic view of the in-progress Forge
builder. This does not complete the Biome API module, M5, or the roadmap's 60%
catalog gate.
