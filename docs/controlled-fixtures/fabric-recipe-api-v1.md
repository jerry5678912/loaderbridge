# Fabric Recipe API v1 controlled fixture

This fixture tests Fabric custom ingredients through parsing, matching,
crafting output, network synchronization, datapack reload, and world reload.
The source fixture uses only Fabric-facing APIs and declares
`fabric-recipe-api-v1`; LoaderBridge's bytecode and metadata inspection
automatically installs Recipe API, Networking API, Lifecycle Events, and API
Base bridges.

Pinned inputs:

- Minecraft `1.21.1`
- Forge `52.1.16`
- Fabric Recipe API contract `5.0.16+2475392c19`
- macOS graphical client and dedicated-server laboratory

The bridge implements the complete public ingredient contract for the pinned
module: `CustomIngredient`, `CustomIngredientSerializer`,
`DefaultCustomIngredients`, and `FabricIngredient`. It registers Fabric's
`all`, `any`, `difference`, `components`, and `custom_data` serializers and
allows mods to register additional serializers dynamically. Fabric's
`fabric:type` JSON is decoded through Minecraft's ingredient codec and backed
by Forge's native custom-ingredient matcher.

The controlled mod registers a third-party count-sensitive ingredient and
checks the built-in boolean ingredient operations. Its datapack recipe uses a
Fabric `any` ingredient to turn either cobblestone or stone into a diamond.
Both the initial server load and `/reload` located that recipe, matched a
cobblestone crafting input, and assembled the diamond output.

The final bridge build's graphical run negotiated all six registered custom
serializers between the integrated server and client, encoded the custom
ingredient natively, loaded 1,291 recipes, created a world, saved every
dimension, reopened the same world, and stopped cleanly. Two additional local
component/custom-data assertions were then added to the fixture without
changing the bridge JAR; that final fixture passed the dedicated-server run
below. If a remote client does not advertise a serializer, the network codec
instead sends the ingredient's vanilla matching-stack fallback; the
negotiation decision is covered by the contract test.

The dedicated server independently loaded 1,291 recipes, proved the diamond
result before and after `/reload`, flushed every dimension, and stopped
cleanly.

Prepared artifact evidence:

- bridge SHA-256: `c3c4b914c27e2eae7b0f400c6e03c1fab9f143bd8cdadad516d6f4b0e52dd783`
- fixture SHA-256: `a30f01eb2b0133778da282a7e2c046ad065d0a9f0ffa1b1dd9a0aea0161aaea7`
- lock SHA-256: `d1230091d657204fcce6d9feb22498eeb53411f90a2fbe41150782419c0dd2ea`

This closes the Recipe API v1 increment of M5. It does not complete M5, prove
Windows/Linux parity, or establish catalog-wide compatibility.
