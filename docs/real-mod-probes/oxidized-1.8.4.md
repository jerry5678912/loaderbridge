# Oxidized 1.8.4 real-mod probe

Status: core technology behavior passes; custom recipe-book category warnings
remain.

## Pinned source

- Repository: Modrinth project `wOZRkmgG` (Oxidized)
- Version: `sxaTrNon`, `1.8.4`
- Source loader: Fabric
- Minecraft: 1.21.1
- Source SHA-1: `230f84079defc3896b561cce6e830e4565f5f839`
- Source SHA-256: `6587c34f7c0887dcf64a3e8213a580fb3558b9804357f29ac6defd4d46e9d634`
- Required dependency: Fabric API `0.116.15+1.21.1`
- Version page: <https://modrinth.com/mod/oxidized/version/sxaTrNon>

On 2026-08-03, the official Modrinth endpoint reported this version with only
the `fabric` loader. The same project returned empty Forge and NeoForge 1.21.1
version lists. No native target-loader artifact was substituted. A frozen
catalog score still requires preserved cross-site identity evidence.

## Failure cluster and generic repair

The first Forge run failed during SafLib block registration because Fabric's
patched Minecraft ABI exposes `BlockEntityType.Builder.build()` while Forge
52.1.x exposes `build(Type<?>)`. LoaderBridge now matches that exact owner,
method, and descriptor in transformed bytecode, inserts the null data-fixer
argument used for untyped block entities, and invokes Forge's runtime shape.
The rule does not match a filename or mod ID.

## Recorded run

- Host: Forge 52.1.0
- Platform: macOS arm64
- Java: 21.0.12
- Prepared artifacts: 11
- Probe block/item: `oxidized:copper_kiln`

The repaired graphical run emitted:

```text
LOADERBRIDGE_CLIENT_TITLE_READY
LOADERBRIDGE_CLIENT_WORLD_READY
LOADERBRIDGE_CONTENT_REGISTRY_READY=oxidized:copper_kiln
LOADERBRIDGE_CONTENT_BLOCK_PLACED=4, 130, -4
LOADERBRIDGE_MACHINE_INPUT_READY=minecraft:clay_ball
LOADERBRIDGE_MACHINE_OUTPUT_READY=minecraft:brick
LOADERBRIDGE_CLIENT_WORLD_SAVED
LOADERBRIDGE_MACHINE_OUTPUT_RELOADED=minecraft:brick
LOADERBRIDGE_CONTENT_BLOCK_RELOADED=oxidized:copper_kiln
LOADERBRIDGE_CONTENT_ITEM_RELOADED=oxidized:copper_kiln
LOADERBRIDGE_CLIENT_WORLD_RELOADED
LOADERBRIDGE_CLIENT_STOPPED
```

The fresh preparation lock SHA-256 is
`ddbf2b87968caf6b58a119d3909ea6bd1343d007692e40251b3ca15a70012743`.
Transformed third-party JARs and machine-specific lock paths are not committed
or redistributed.

## Remaining client UI gap

The deep scenario placed the kiln, inserted `minecraft:clay_ball` and
`minecraft:coal`, waited for the real block entity to produce
`minecraft:brick`, saved, and verified that output in the reloaded machine.
This proves the mod's core kiln behavior rather than startup alone.

The client still logs unknown `oxidized:kiln_smelting` recipe-book categories.
That does not prevent server recipe processing, but recipe-book presentation is
a known unpassed UI behavior and remains a separate compatibility repair.
