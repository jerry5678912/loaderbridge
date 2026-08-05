# Fabric Tag API v1 controlled gate

Pinned contract: `fabric-tag-api-v1:1.3.0+1eb36c0719` from Fabric API
`0.116.15+1.21.1`.

The bridge injects Fabric's `FabricTagFile` interface into Minecraft's tag-file
record. During tag resource parsing it aliases the structured `fabric:remove`
array into Forge's existing `remove` field. If both valid arrays exist, their
entries are merged in source order. A malformed Fabric or Forge removal field
is left for the native codec to reject.

The lifecycle fixture declares the module in metadata and packages
`data/loaderbridge/tags/item/tag_removal_fixture.json`. That tag first adds
stone and dirt, then removes dirt through `fabric:remove`. Automatic inspection
selected Tag API, API Base, and Resource Loader without using a filename or
mod-ID special case.

On 2026-08-06, a disposable Forge 52.1.16 dedicated server proved:

- stone remained in the live item tag and dirt did not;
- `LOADERBRIDGE_FABRIC_TAG_API_READY removed=minecraft:dirt` appeared during
  initial resource loading;
- the server reached ready, saved the overworld, Nether, and End, and stopped
  cleanly;
- a second JVM reopened the saved world, repeated the exact tag assertion,
  saved all dimensions, and stopped cleanly;
- no Mixin transformer exception occurred in either accepted run.

This closes the pinned Tag API v1 module gate. It does not complete M5 or its
catalog-wide 60% behavioral score.
