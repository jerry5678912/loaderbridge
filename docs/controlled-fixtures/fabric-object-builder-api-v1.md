# Fabric Object Builder API v1 controlled gate

Pinned contract: `fabric-object-builder-api-v1:15.2.1+40875a9319` from Fabric
API `0.116.15+1.21.1`.

Revision 7 adds the pinned `FabricBlockEntityType` interface and its nested
vanilla-builder interface. LoaderBridge injects them into Minecraft's real
`BlockEntityType` and `BlockEntityType.Builder` classes before construction.
Every constructed type receives a private mutable copy of its supported-block
set, while `Builder.build()` delegates to Minecraft's datafixer-aware overload
with no datafixer type.

The translated lifecycle fixture proves both paths by adding oak planks to an
already built type and by invoking the injected no-argument method on a native
builder. It registers the resulting test type during LoaderBridge's existing
Fabric registration window before Forge freezes intrusive holders.

On 2026-08-06, the exact assertions passed in fresh and saved-world processes
on Forge 52.1.0 and 52.1.16. Each process completed the broader lifecycle
fixture, reached dedicated-server ready, saved the Overworld, Nether, and End,
and stopped cleanly. Remaining Object Builder public types stay unadvertised;
this is an increment, not the complete module or M5 gate.
