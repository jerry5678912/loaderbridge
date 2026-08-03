# Blockus 2.9.18+1.21.1 real-mod probe

Status: passing on the recorded matrix entry.

## Pinned source

- Repository: Modrinth project `zFiY2Go0` (Blockus)
- Version: `BduJoveu`, `2.9.18+1.21.1`
- Source loader: Fabric
- Minecraft: 1.21.1
- Source SHA-1: `5867957abcc1c175471b52c33bfe8cc926cc4fe4`
- Source SHA-256: `f2db8c0e9c320326ddb490419d28027724439806e8e2fca1d55d20f57451b774`
- Version page: <https://modrinth.com/mod/blockus/version/BduJoveu>

On 2026-08-03, the official Modrinth version endpoint reported only the
`fabric` loader for this version. Querying the same project for Minecraft
1.21.1 and loader `forge` returned an empty version list. No native Forge
artifact was substituted. A catalog snapshot must additionally preserve any
cross-site identity evidence before this result contributes to the final 95%
score.

## Recorded run

- Host: Forge 52.1.0
- Platform: macOS arm64
- Java: 21.0.12
- Prepared artifacts: 17, automatically selected from JAR metadata and
  bytecode references
- Probe block/item: `blockus:amethyst_bricks`

The graphical test reached the title screen and then emitted all required
behavioral markers:

```text
LOADERBRIDGE_CONTENT_REGISTRY_READY=blockus:amethyst_bricks
LOADERBRIDGE_CONTENT_BLOCK_PLACED=12, 64, -45
LOADERBRIDGE_CLIENT_WORLD_SAVED
LOADERBRIDGE_CONTENT_BLOCK_RELOADED=blockus:amethyst_bricks
LOADERBRIDGE_CONTENT_ITEM_RELOADED=blockus:amethyst_bricks
LOADERBRIDGE_CLIENT_WORLD_RELOADED
LOADERBRIDGE_CLIENT_STOPPED
```

The corresponding immutable preparation was regenerated against Forge 52.1.16
after the passing run. Its local lock SHA-256 was
`c4ed329117e8510722cfac3d3b0d2207126962e2acb56857356b8062cb440f53`.
Transformed third-party JARs and machine-specific lock paths are not committed
or redistributed.

## Scope of this result

This proves one Fabric-only content/building mod on one graphical platform and
one host build. It does not prove catalog-wide compatibility, Windows/Linux
parity, the latest Forge runtime matrix entry, or a 95% score. Those remain
separate measured gates.
