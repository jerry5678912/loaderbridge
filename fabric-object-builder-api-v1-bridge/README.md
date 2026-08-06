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
- `PointOfInterestHelper`, `VillagerProfessionBuilder`, and
  `VillagerTypeHelper`, backed by Forge's POI state index and biome-type hook.

Unimplemented public types remain unadvertised so preprocessing reports the stable
missing-Fabric-API diagnostic instead of installing an incomplete bridge silently.

Revision 9 passed saved-world and restart dedicated-server runs on Forge 52.1.0
and 52.1.16. The fixture verifies POI ticket/range data and block-state lookup,
profession workstation/item/secondary-site behavior, and biome-to-villager
type selection. It saves all dimensions and repeats every assertion after a
process restart.
