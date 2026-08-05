# Fabric Item API v1 bridge

This module targets `fabric-item-api-v1` 11.3.0+467044f319 for Minecraft
1.21.1. Revision 1 provides the pinned common item contracts used by the
controlled content fixture:

- `FabricItem`, `FabricItem.Settings`, and `FabricItemStack`;
- `CustomDamageHandler` and `EquipmentSlotProvider`;
- `FabricComponentMapBuilder` and `FabricTooltipType`.

Early Mixins inject those contracts into Minecraft's `Item`,
`Item.Properties`, `ItemStack`, `DataComponentMap.Builder`, `TooltipFlag`, and
equipment-slot path. This preserves Fabric-defined per-stack recipe remainders,
creator namespaces, custom durability handling, equipment-slot selection, and
component-builder helpers. The runtime module is discovered through
`ServiceLoader` and selected from inspected bytecode references rather than
artifact names.

This is deliberately not advertised as the complete pinned module yet.
Default-item-component mutation, vanilla enchanting event hooks, recipe-manager
integration for stack-aware remainders, client tooltip and animation callbacks,
and special creator namespaces remain unimplemented and therefore are not
listed as provided reference surfaces.

The controlled fixture registers a real Fabric item with working model and
language resources. Its client and dedicated-server evidence is documented in
`docs/controlled-fixtures/fabric-item-api-v1.md`.
