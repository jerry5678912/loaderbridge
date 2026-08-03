# Upstream source audit

LoaderBridge uses upstream source to derive lifecycle behavior, then verifies
the decision against the exact pinned runtime and real-mod probes.

## User-supplied archives

- `fabric-loader-master.zip` identifies commit
  `b907c5b292fc062d75b6d8bf8255ac200109b992`. Its Minecraft `Hooks` invokes
  `main` before the active `client` or `server` entrypoint.
- `MinecraftForge-26.2.zip` identifies commit
  `d17cfd0b4bbfd192a9007f02240032f19b9b340d`. This is Forge for Minecraft
  26.2, not LoaderBridge's Minecraft 1.21.1 target, so it is architectural
  reference material rather than the target ABI authority.

The upstream repositories are
[Fabric Loader](https://github.com/FabricMC/fabric-loader) and
[MinecraftForge](https://github.com/MinecraftForge/MinecraftForge).

## Exact 1.21.1 authority

The implementation is compiled and tested against the pinned Maven artifacts
for Forge `1.21.1-52.1.0`, including its published source JAR. That exact source
shows:

- `FMLConstructModEvent` runs before Forge creates and populates its registries.
- Forge posts registry events before `FMLCommonSetupEvent`.
- `ModelEvent.RegisterAdditional` loads the model resource named by its model
  location directly, while vanilla item discovery loads `models/item/<id>` and
  stores it under the logical inventory model ID.
- `ItemRenderer` snapshots the item registry in its constructor and later
  rebuilds baked models from that item-to-model table.

Running all Fabric entrypoints during Forge construction was tested and
rejected: Oxidized reads Forge's `swim_speed` attribute while creating entity
attributes, but that holder is not bound until Forge's registry population.
LoaderBridge therefore retains the post-registry entrypoint window and repairs
the client model discovery/cache boundary generically.

## Behavioral proof

The Oxidized probe now rejects missing block models, missing inventory models,
and missing textures before it performs its machine scenario. It then places
the kiln, processes clay and coal into a brick, saves, reloads, and verifies the
block, inventory item, and machine output. The separate custom recipe-book
category warning is still open and is not hidden by this result.
