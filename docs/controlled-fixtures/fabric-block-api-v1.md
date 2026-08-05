# Fabric Block API v1 controlled fixture

This controlled Fabric-only content mod tests pinned
`fabric-block-api-v1:1.1.0+0bc3503219` through LoaderBridge on Minecraft 1.21.1
and Forge 52.1.16.

The ordinary `prepare` command inspected class references and automatically
selected only `fabric-block-api-v1-bridge`; no filename or mod-ID rule chose
the adapter. At runtime the fixture proves that vanilla blocks and block states
implement their Fabric interfaces, then registers a mimic block whose
`FabricBlock#getAppearance` returns a gold-block state and invokes it through
`FabricBlockState`.

The fixture is also a small content-mod gate. It registers a matching block
item and packages a valid blockstate, cube model using the vanilla gold texture,
inventory model, and English name. The final graphical run had no
`mimic_block` missing-model warning.

The Forge 52.1.16 graphical scenario used a clean world and reached:

- `LOADERBRIDGE_FABRIC_BLOCK_API_READY`;
- `LOADERBRIDGE_FABRIC_BLOCK_CONTENT_READY`;
- title screen and integrated-world ready;
- world save, clean disconnect, and same-process world reload;
- clean client and integrated-server shutdown.

Two dedicated-server processes used the same prepared artifacts. The first
created and saved `loaderbridge-block-api-server-r1`; the second loaded that
world. Both emitted the API and content markers, reached `Done`, saved all
dimensions, and stopped cleanly.

Repeated preparation was byte-identical:

- bridge SHA-256: `70c41760f34bee43983929ae344028adc6c229ffbab4cad1f0193c3e9310a3d4`
- fixture SHA-256: `f20dc8c782d25fd36d4592b979bf6d38a8cd60a3df43e19fd621db6ecee93af5`
- lock SHA-256: `71e04d58735043f6536272db5a03472522b34d6ef4225ea2298c203e2adca393`

This closes the pinned Fabric Block API v1 module gate. It does not complete
M5, establish the 60% catalog score, or claim arbitrary content-mod support.
