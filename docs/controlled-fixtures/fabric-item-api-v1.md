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

Behavioral revision 17 adds the Content Registries revision 2 brewing bridge.
The fixture registers a Fabric potion recipe that uses its content item to turn
water into awkward potions. It places a real brewing stand with three water
potions, the Fabric ingredient, and blaze powder, advances the native block
entity, and requires all three awkward outputs. The ingredient slot retains a
gold nugget and the same callback counter proves the remainder method runs once.

Revision 18 adds Fabric's enchanting events. An ordered tri-state callback
allows Sharpness on the otherwise unsupported Fabric content item. A real
`EnchantmentHelper.getAvailableEnchantmentResults` call proves the PRIMARY
table path, and the server's `/enchant` command proves the ACCEPTABLE path by
applying Sharpness I to a spawned armor stand's held item. The modify callback
identifies Sharpness as a vanilla resource and adds an XP-repair effect; the
live `modifyDurabilityToRepairFromXp` result changes from one to four.

Revision 19 adds the special creator-attribution paths. The fixture registers
`loaderbridge_item_api_fixture:creator_potion` and requires both a potion and a
tipped-arrow stack carrying it to report the fixture namespace. Its custom
data-driven `creator_enchantment` is observed by the modify callback with
`EnchantmentSource.MOD`; a book containing exactly that stored enchantment must
also report the fixture namespace. Ordinary item attribution remains covered
by `fabric_tool`.

The fixture is also a content-mod gate. It registers
`loaderbridge_item_api_fixture:fabric_tool` and packages a valid inventory
model using a vanilla gold-ingot texture plus an English translation. The final
graphical logs contained no missing-model or missing-texture warning for the
item.

The Forge 52.1.16 graphical scenario used a clean world and reached:

- `LOADERBRIDGE_FABRIC_ITEM_CONTENT_READY` before the title screen;
- `LOADERBRIDGE_FABRIC_ITEM_API_READY damage=2,slot=head,glint=true,remainder=gold_nugget,furnace=stone,brewing=awkward,enchanting=sharpness,creator=potion+book` on both integrated
  server starts;
- title screen and integrated-world ready;
- world save, clean disconnect, and same-process world reload;
- clean client and integrated-server shutdown.

Dedicated-server processes used the same prepared artifacts and existing saved
world. Two independent processes each emitted both markers, reached `Done`,
executed the recipe, furnace, brewing, table, command, and modified-enchantment
assertions, and stopped through the server console with all dimensions saved.

Repeated preparation was byte-identical:

- API Base bridge SHA-256:
  `338872b4c7690bc9c784275502f0b13b094db29a46fc0f881d141c79d01b2e32`
- Content Registries bridge SHA-256:
  `4ed6126734be46dae847446281e47776671675e8b1aba57b5748e43416fc2b19`
- Item API bridge revision 19 SHA-256:
  `19b51d90a036d71f67d61bb65227689fee4e6ff313f30d58a32b5814a05fabb1`
- Lifecycle bridge SHA-256:
  `4c60518155e5402b78a764c2804ea8fe01e530c0a05ba6e6357c00a97ae472fa`
- transformed fixture revision 19 SHA-256:
  `48b02c9ba0952e9df1bee06ffcc85133eee84e0174264f6d9ce5355326492317`

This closes the common/server enchanting increment along with crafting,
furnace, direct custom-brewing remainders, and registry-aware creator
attribution. Client tooltip/animation behavior remains open for M6, so this does
not close M5 or the 60% catalog gate.
