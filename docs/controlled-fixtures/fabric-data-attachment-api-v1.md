# Fabric Data Attachment API v1 controlled fixture

This fixture tests the pinned `fabric-data-attachment-api-v1:1.4.7+5b36e0f719`
contract through LoaderBridge revision 5 on Minecraft 1.21.1 and Forge 52.1.16.

The source fixture is compiled as a Fabric mod and transformed by the ordinary
`prepare` command. Content inspection automatically selected Data Attachment
API plus API Base, Entity Events, Lifecycle Events, Networking, and Object
Builder. No module was selected from a filename.

The dedicated-server scenario proves the first storage and persistence wave:

- all four official target families are injected: entity, block entity, level,
  and chunk;
- default initialization, get, set, has, modify, remove, and throw semantics;
- identity-keyed attachment types and codec-backed NBT serialization;
- an actual zombie entity save/load round trip preserving integer value `19`;
- an actual chest block-entity save/load round trip preserving value `23`;
- level mutation from `7` to `8`;
- chunk value `31` and the required dirty flag;
- server-level SavedData value `41` and chunk-serializer value `43` written in
  a completely fresh world;
- explicit all-dimension flush and clean process exit;
- a second server process restoring both `41` and `43` from disk through the
  LevelChunk/ImposterProtoChunk wrapper path.

The runtime marker was:

`LOADERBRIDGE_DATA_ATTACHMENT_BASE_READY entity=19 block=23 level=8 chunk=31`

`LOADERBRIDGE_DATA_ATTACHMENT_PERSIST_INIT level=41 chunk=43`

`LOADERBRIDGE_DATA_ATTACHMENT_PERSIST_RELOAD level=41 chunk=43`

Prepared artifact evidence:

- bridge SHA-256: `724deb796f6c32d9bfd56c58b900acccccb4e66d76e68453e7faadcc7933eb39`
- fixture SHA-256: `9c8afdc8c92ffa4b2e8adf197738f0c5d43d40ab3c8daadc46f7bf519419fd14`
- lock SHA-256: `5ff23a1d5eccb90b052bc722323096084677c23fd25bf110c183074e1b5fbd89`

Unit contracts additionally prove persistent-only serialization, registry
lookup, built-in synchronization predicates, null/error semantics, and
copy-on-death filtering. Runtime callbacks transfer attachments on respawn,
entity world replacement, and mob conversion using the completed Entity Events
bridge.

This evidence does not yet prove a generated ProtoChunk-to-LevelChunk transfer,
client synchronization, or client-side attachment application. Those are
explicit open gates before Data Attachment API v1 is called complete. It also
does not complete M5 or establish the roadmap's 60% catalog gate.
