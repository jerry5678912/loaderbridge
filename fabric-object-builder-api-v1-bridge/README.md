# Fabric Object Builder API v1 bridge

This module implements the public binary contracts from
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
- `PointOfInterestHelper`, `VillagerProfessionBuilder`, and
  `VillagerTypeHelper`, backed by Forge's POI state index and biome-type hook.
- The deprecated `FabricBlockSettings` factory, Fabric/Yarn builder aliases,
  covariant returns, and deep-copy semantics. A narrow access transform permits
  the required subclass constructor, while Mixin accessors copy every pinned
  vanilla and Forge property without loading mod classes during preprocessing.

Revision 10 completes the pinned module's public API surface. It passed
saved-world and restart dedicated-server runs on Forge 52.1.0 and 52.1.16. The
fixture verifies POI and profession behavior, villager-type selection, minecart
comparator logic, and a registered block copied through `FabricBlockSettings`,
including hardness, resistance, movement factors, light, fire, collision,
map color, piston reaction, and loot-table state. It saves all dimensions and
repeats every assertion after a process restart.
