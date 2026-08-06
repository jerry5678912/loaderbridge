# Fabric Object Builder API v1 bridge

This module implements selected binary contracts from
`fabric-object-builder-api-v1` 15.2.1+40875a9319 for Minecraft 1.21.1.

Implemented now:

- `FabricBlockEntityTypeBuilder`, delegated to the official Minecraft builder.
- `FabricBlockEntityType`, injected into every vanilla block-entity type so
  supported blocks can be added after construction, plus its vanilla builder
  extension for no-datafixer builds.
- `FabricDefaultAttributeRegistry`, transferred through Forge's native entity
  attribute creation event.
- `FabricEntityTypeBuilder` base, living, and mob specializations, including
  tracking options, attributes, Fabric-compatible unnamed builds, and native
  spawn restrictions.
- `FabricEntityType.Builder`, injected into Minecraft's builder with modern
  living/mob configuration callbacks and Forge-backed velocity updates.
- `MinecartComparatorLogic` and `MinecartComparatorLogicRegistry`, with
  identity-keyed callbacks evaluated by powered detector rails before vanilla
  comparator fallback.

Unimplemented public types remain unadvertised so preprocessing reports the stable
missing-Fabric-API diagnostic instead of installing an incomplete bridge silently.

Revision 8 passed fresh and saved-world dedicated-server runs on Forge 52.1.0
and 52.1.16. In addition to the block-entity checks, the fixture registers
custom minecart logic, places a powered detector rail, spawns a real minecart,
and observes comparator output `11`. It saves all dimensions and repeats the
same behavioral assertion after a process restart.
