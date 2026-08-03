# Fabric Object Builder API v1 bridge

This module implements selected binary contracts from
`fabric-object-builder-api-v1` 15.2.1+40875a9319 for Minecraft 1.21.1.

Implemented now:

- `FabricBlockEntityTypeBuilder`, delegated to the official Minecraft builder.
- `FabricDefaultAttributeRegistry`, transferred through Forge's native entity
  attribute creation event.

Unimplemented public types remain unadvertised so preprocessing reports the stable
missing-Fabric-API diagnostic instead of installing an incomplete bridge silently.
