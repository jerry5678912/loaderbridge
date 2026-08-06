# Fabric Object Builder API v1 controlled gate

Pinned contract: `fabric-object-builder-api-v1:15.2.1+40875a9319` from Fabric
API `0.116.15+1.21.1`.

Revision 10 adds the final pinned public type, the deprecated
`FabricBlockSettings`. LoaderBridge exposes its Fabric/Yarn aliases and
covariant builder methods, and copies the complete property state through an
early constructor access transform plus field-level Mixin accessors. Earlier
block-entity, entity, trade, POI, villager, block/wood type, and minecart
comparator behavior remains in place.

The translated lifecycle fixture creates a two-ticket, three-block-range POI
for a custom block, requires Minecraft's real block-state lookup to find it,
and verifies a custom profession's workstation predicate, requested item, and
secondary site. It maps Plains to a custom villager type and checks the live
biome holder resolves that type. It also registers source and copied blocks and
asserts copied hardness, resistance, friction, movement factors, luminance,
fire behavior, collision, map color, piston reaction, and loot-table identity.

On 2026-08-06, the exact assertions passed in fresh and saved-world processes
on Forge 52.1.0 and 52.1.16. Each process completed the broader lifecycle
fixture, reached dedicated-server ready, saved the Overworld, Nether, and End,
and stopped cleanly. This completes the pinned Object Builder API v1 module,
but it is one module-level gate within the broader M5 Fabric API milestone.
