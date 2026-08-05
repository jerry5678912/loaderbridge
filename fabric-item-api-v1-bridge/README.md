# Fabric Item API v1 bridge

This module targets `fabric-item-api-v1` 11.3.0+467044f319 for Minecraft
1.21.1. Revision 1 provides the pinned common item contracts used by the
controlled content fixture:

- `FabricItem`, `FabricItem.Settings`, and `FabricItemStack`;
- `DefaultItemComponentEvents`, including item, collection, and predicate
  mutation contexts;
- `EnchantingContext`, `EnchantmentEvents`, and `EnchantmentSource`;
- `CustomDamageHandler` and `EquipmentSlotProvider`;
- `FabricComponentMapBuilder` and `FabricTooltipType`.

Early Mixins inject those contracts into Minecraft's `Item`,
`Item.Properties`, `ItemStack`, `DataComponentMap.Builder`, `TooltipFlag`, and
equipment-slot path. This preserves Fabric-defined per-stack recipe remainders,
creator namespaces, custom durability handling, equipment-slot selection, and
component-builder helpers. The runtime module is discovered through
`ServiceLoader` and selected from inspected bytecode references rather than
artifact names.

Forge's crafting, furnace, and brewing implementations already call generic
stack-aware remainder hooks. The bridge delegates those hooks to Fabric and
carries an identity-bound result between Forge's separate predicate and
retrieval calls so a mod callback executes once.

Default component callbacks run after Fabric initialization on either Forge
lifecycle ordering. Mutations update vanilla's backing component map and
Forge's gathered-component cache together, preserving components contributed
by either loader.

Revision 4 routes ordered Fabric tri-state enchanting decisions through
Forge's table filter and Minecraft's anvil, command, and compatible random-loot
paths. Dynamic enchantment loading invokes Fabric's modify event after copying
the original definition, exclusive set, special effects, and list effects into
a mutable builder. Resource provenance is reported as vanilla, mod, or external
data pack. The controlled fixture proved PRIMARY table filtering, ACCEPTABLE
`/enchant` execution, and a modified Sharpness XP-repair effect in live worlds.

This is deliberately not advertised as the complete pinned module yet.
Client tooltip and animation callbacks and special creator namespaces remain
unimplemented and therefore are not listed as provided reference surfaces.

The controlled fixture registers a real Fabric item with working model and
language resources. Its client and dedicated-server evidence is documented in
`docs/controlled-fixtures/fabric-item-api-v1.md`.
