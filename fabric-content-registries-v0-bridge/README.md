# Fabric Content Registries v0 bridge

This module targets `fabric-content-registries-v0` 8.0.19+b559734419 for
Minecraft 1.21.1. Revision 1 implements the item/block override maps used by
fuel, composting, flammability, stripping, flattening, oxidation, and waxing.
Forge events and early Mixins route vanilla behavior through the registered
Fabric values.

Revision 2 adds the exact `FabricBrewingRecipeRegistryBuilder` and nested build
callback contracts. Forge's brewing-registration event supplies its native
builder to Fabric callbacks, while an injected builder implementation adds
item mixes, potion mixes, bulk water/awkward recipes, and feature gating with
the same target recipe lists. A translated fixture brewed real water potions
into awkward potions, retained its Fabric-defined ingredient remainder, and
passed graphical plus two-process dedicated-server save/reload gates.

Unimplemented path-node, sculk, tilling, and villager interaction contracts
remain unadvertised and gated.
