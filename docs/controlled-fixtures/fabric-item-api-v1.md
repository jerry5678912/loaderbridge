# Fabric Item API v1 controlled fixture

This controlled Fabric-only content mod tests the first common slice of pinned
`fabric-item-api-v1:11.3.0+467044f319` through LoaderBridge on Minecraft 1.21.1
and Forge 52.1.16.

The ordinary `prepare` command inspected class references and automatically
selected the Item API, API Base, Content Registries, and Lifecycle bridges; no
filename or mod-ID rule chose an adapter. The fixture proves that Minecraft
items, stacks,
component builders, properties, and tooltip flags implement their exact Fabric
interfaces. It registers a real Fabric item whose settings select the head
equipment slot and replace five durability damage with two, and whose stack
returns a custom recipe remainder and creator namespace.

Revision 15 adds `DefaultItemComponentEvents`. The fixture registers a callback
that gives the content item a default enchantment-glint override. Forge caches
gathered item components separately from vanilla's component field, so the
bridge preserves the gathered map and updates both storage locations. The
fixture creates a new stack at server start and requires the default glint
component to be present.

Revision 16 connects Fabric's stack-aware recipe remainder to Forge's generic
stack hooks. The fixture passes a Fabric item, a vanilla water bucket, and an
ordinary diamond through a real `Recipe.getRemainingItems` invocation. It
requires a gold nugget, bucket, and empty stack respectively, while a callback
counter proves the Fabric item method runs once despite Forge's separate
`hasCraftingRemainingItem` and `getCraftingRemainingItem` calls. The fixture
also registers the Fabric item as fuel through `FuelRegistry`, places a real
furnace block entity in the world, cooks cobblestone for 200 ticks, and requires
stone output plus the gold-nugget remainder.

The fixture is also a content-mod gate. It registers
`loaderbridge_item_api_fixture:fabric_tool` and packages a valid inventory
model using a vanilla gold-ingot texture plus an English translation. The final
graphical logs contained no missing-model or missing-texture warning for the
item.

The Forge 52.1.16 graphical scenario used a clean world and reached:

- `LOADERBRIDGE_FABRIC_ITEM_CONTENT_READY` before the title screen;
- `LOADERBRIDGE_FABRIC_ITEM_API_READY damage=2,slot=head,glint=true,remainder=gold_nugget,furnace=stone` on both integrated
  server starts;
- title screen and integrated-world ready;
- world save, clean disconnect, and same-process world reload;
- clean client and integrated-server shutdown.

Dedicated-server processes used the same prepared artifacts and existing saved
world. Two independent processes each emitted both markers, reached `Done`,
executed the recipe and furnace assertions, and stopped through the server
console with all dimensions saved.

Repeated preparation was byte-identical:

- API Base bridge SHA-256:
  `338872b4c7690bc9c784275502f0b13b094db29a46fc0f881d141c79d01b2e32`
- Content Registries bridge SHA-256:
  `6bf6c9497b292a180051d1f3455565b65a2a0395148b817b43d389aab874f650`
- Item API bridge revision 16 SHA-256:
  `eb178a964f12784b093a1d8dff3efd9c3db26cc620c95c89518f8d674380aa52`
- Lifecycle bridge SHA-256:
  `4c60518155e5402b78a764c2804ea8fe01e530c0a05ba6e6357c00a97ae472fa`
- transformed fixture revision 16 SHA-256:
  `efe4d47cca7c1fd9149b2b6c6510d8995fe1731bb2d37ecf17095df55f4ca05d`

This closes the crafting and furnace remainder increment. Forge's brewing path
uses the same generic stack hook, but a direct custom Fabric brewing scenario
still awaits the corresponding brewing-registry bridge. Full enchanting and
client tooltip/animation behavior also remain open, so this does not close the
entire module, M5, or the 60% catalog gate.
