# Fabric Object Builder API v1 controlled gate

Pinned contract: `fabric-object-builder-api-v1:15.2.1+40875a9319` from Fabric
API `0.116.15+1.21.1`.

Revision 8 adds the pinned `MinecartComparatorLogic` and
`MinecartComparatorLogicRegistry` contracts. LoaderBridge preserves Fabric's
identity-keyed entity-type registry and injects a detector-rail hook that asks
registered logic before falling back to vanilla comparator behavior. Revision
7's injected `FabricBlockEntityType` and vanilla-builder interfaces remain in
place.

The translated lifecycle fixture also registers minecart logic returning `11`,
places a powered detector rail in the live Overworld, spawns a real minecart in
its query box, and requires the rail's analog output to equal `11`. Its earlier
block-entity paths still mutate an already built type and register a native
builder result before Forge freezes intrusive holders.

On 2026-08-06, the exact assertions passed in fresh and saved-world processes
on Forge 52.1.0 and 52.1.16. Each process completed the broader lifecycle
fixture, reached dedicated-server ready, saved the Overworld, Nether, and End,
and stopped cleanly. Remaining Object Builder public types stay unadvertised;
this is an increment, not the complete module or M5 gate.
