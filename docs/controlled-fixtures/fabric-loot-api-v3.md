# Fabric Loot API v3 controlled fixture

This fixture tests behavior, not merely startup. The source fixture uses only
the Fabric-facing API and declares `fabric-loot-api-v3`; LoaderBridge inspects
its bytecode and metadata, then automatically installs the Loot API, API Base,
and Resource Loader bridges.

Pinned inputs:

- Minecraft `1.21.1`
- Forge `52.1.16`
- Fabric Loot API contract `1.0.3+3f89f5a519`
- macOS client and dedicated-server laboratory

The fixture's `REPLACE` listener verifies that
`minecraft:blocks/cobblestone` has the `VANILLA` source and replaces it with a
diamond pool. Its `MODIFY` listener verifies the next source is `REPLACED` and
adds an emerald pool. `ALL_LOADED` verifies the final registry contains the
target table.

The graphical client executed that final loot table and required both a diamond
and an emerald. It added the real results to the player inventory, saved every
dimension, reopened the world, verified both items persisted, and stopped
cleanly. The dedicated server reached ready, repeated all three callbacks on
`/reload`, saved all dimensions, and stopped cleanly.

Prepared artifact evidence:

- bridge SHA-256: `7498afa275b2cf990c2cd7406319a0524ff1021eb22bdd8d5b668f444126eedf`
- fixture SHA-256: `712f6aad5b583ccfe52998c8bf8c36d182669c4d57e6b87bca0ca49a08a1636f`
- lock SHA-256: `e641934d93da2f7e7cc1ad01e84d1f3a11d7e38ca60a9d7fec024f7837bacbae`

This closes the Loot API v3 increment of M5. It does not complete M5, prove
Windows/Linux parity, or establish catalog-wide compatibility.
