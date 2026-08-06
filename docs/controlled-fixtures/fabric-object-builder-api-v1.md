# Fabric Object Builder API v1 controlled gate

Pinned contract: `fabric-object-builder-api-v1:15.2.1+40875a9319` from Fabric
API `0.116.15+1.21.1`.

Revision 9 adds the pinned `PointOfInterestHelper`,
`VillagerProfessionBuilder`, and `VillagerTypeHelper` contracts. LoaderBridge
registers POIs through Forge's state-indexed registry path and maps villager
types to biomes through Forge's public 1.21.1 hook. Earlier block-entity and
minecart comparator behavior remains in place.

The translated lifecycle fixture creates a two-ticket, three-block-range POI
for a custom block, requires Minecraft's real block-state lookup to find it,
and verifies a custom profession's workstation predicate, requested item, and
secondary site. It maps Plains to a custom villager type and checks the live
biome holder resolves that type.

On 2026-08-06, the exact assertions passed in fresh and saved-world processes
on Forge 52.1.0 and 52.1.16. Each process completed the broader lifecycle
fixture, reached dedicated-server ready, saved the Overworld, Nether, and End,
and stopped cleanly. Remaining Object Builder public types stay unadvertised;
this is an increment, not the complete module or M5 gate.
