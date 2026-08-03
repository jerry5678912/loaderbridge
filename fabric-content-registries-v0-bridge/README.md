# Fabric Content Registries v0 bridge

This module targets `fabric-content-registries-v0` 8.0.19+b559734419 for
Minecraft 1.21.1. Revision 1 implements the item/block override maps used by
fuel, composting, flammability, stripping, flattening, oxidation, and waxing.
Forge events and early Mixins route vanilla behavior through the registered
Fabric values. Unimplemented brewing, path-node, sculk, tilling, and villager
interaction contracts remain unadvertised and gated.
