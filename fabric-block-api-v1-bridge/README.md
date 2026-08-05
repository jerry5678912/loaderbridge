# Fabric Block API v1 bridge

This module targets `fabric-block-api-v1` 1.1.0+0bc3503219 for Minecraft
1.21.1. It provides the complete pinned public surface:

- `FabricBlock`, with Fabric's `BlockGetter`-based appearance signature;
- `FabricBlockState`, delegating appearance queries to the owning block;
- `BlockFunctionalityTags.CAN_CLIMB_TRAPDOOR_ABOVE`.

Required Mixins inject the two interfaces into every Minecraft `Block` and
`BlockState`. The exact Fabric signature is retained instead of substituting
Forge's similar `BlockAndTintGetter` overload, so Fabric overrides link and
dispatch correctly. The runtime module is discovered through `ServiceLoader`
and selected from inspected bytecode references rather than artifact names.

The controlled fixture registers a real block and block item and includes
working blockstate, block-model, item-model, and language assets. Its client
and dedicated-server evidence is documented in
`docs/controlled-fixtures/fabric-block-api-v1.md`.
