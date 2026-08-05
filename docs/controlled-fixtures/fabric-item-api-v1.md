# Fabric Item API v1 controlled fixture

This controlled Fabric-only content mod tests the first common slice of pinned
`fabric-item-api-v1:11.3.0+467044f319` through LoaderBridge on Minecraft 1.21.1
and Forge 52.1.16.

The ordinary `prepare` command inspected class references and automatically
selected the Item API, API Base, and Lifecycle bridges; no filename or mod-ID
rule chose an adapter. The fixture proves that Minecraft items, stacks,
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

The fixture is also a content-mod gate. It registers
`loaderbridge_item_api_fixture:fabric_tool` and packages a valid inventory
model using a vanilla gold-ingot texture plus an English translation. The final
graphical logs contained no missing-model or missing-texture warning for the
item.

The Forge 52.1.16 graphical scenario used a clean world and reached:

- `LOADERBRIDGE_FABRIC_ITEM_CONTENT_READY` before the title screen;
- `LOADERBRIDGE_FABRIC_ITEM_API_READY damage=2,slot=head,glint=true` on both integrated
  server starts;
- title screen and integrated-world ready;
- world save, clean disconnect, and same-process world reload;
- clean client and integrated-server shutdown.

Dedicated-server processes used the same prepared artifacts. The first created
and saved `loaderbridge-item-api-server-r1`; subsequent processes loaded that
world without regeneration. Each emitted both markers, reached `Done`, and the
captured final reload stopped through the server console with all dimensions
saved.

Repeated preparation was byte-identical:

- API Base bridge SHA-256:
  `338872b4c7690bc9c784275502f0b13b094db29a46fc0f881d141c79d01b2e32`
- Item API bridge revision 15 SHA-256:
  `1e496917de5ccaef165562a032fa011840d56583661ad1c10ae92d37d7ec3754`
- Lifecycle bridge SHA-256:
  `4c60518155e5402b78a764c2804ea8fe01e530c0a05ba6e6357c00a97ae472fa`
- transformed fixture revision 15 SHA-256:
  `f27b6f9c99c0cc29abecd2a60153edd11a4bd34d1a1ffe0f6719894003dfa938`

This closes the default-component increment. Full enchanting integration,
recipe-pipeline remainder handling, and client tooltip/animation behavior
remain open, so this does not close the entire module, M5, or the 60% catalog
gate.
