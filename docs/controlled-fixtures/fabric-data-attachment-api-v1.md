# Fabric Data Attachment API v1 controlled fixture

This fixture tests the pinned `fabric-data-attachment-api-v1:1.4.7+5b36e0f719`
contract through LoaderBridge revision 7 on Minecraft 1.21.1 and Forge 52.1.16.

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

The graphical integrated-server scenario proves the first wire-synchronization
wave:

- attachment values are encoded by each registered Fabric packet codec;
- level, player/entity, block-entity, and chunk targets have structured network
  addresses rather than filename or mod-ID rules;
- join-time level/player, entity-tracking, and chunk-watch snapshots are wired;
- live mutations select tracking recipients and honor the Fabric sync
  predicate;
- the client resolves targets and applies synchronized values on its game
  thread;
- the client observed level value `53` from initial synchronization and player
  value `59` from mutation synchronization in the fresh session;
- after save and world reopen, a new client player observed `53/59` again as
  synchronization session 2;
- before each play session, a configuration task negotiated exactly the two
  syncable fixture attachment IDs and blocked until the response arrived;
- the integrated server simultaneously restored persistent values `41/43` and
  the graphical process stopped cleanly.

The runtime marker was:

`LOADERBRIDGE_DATA_ATTACHMENT_BASE_READY entity=19 block=23 level=8 chunk=31`

`LOADERBRIDGE_DATA_ATTACHMENT_PERSIST_INIT level=41 chunk=43`

`LOADERBRIDGE_DATA_ATTACHMENT_PERSIST_RELOAD level=41 chunk=43`

`LOADERBRIDGE_DATA_ATTACHMENT_CLIENT_SYNC_READY level=53 player=59 session=1`

`LOADERBRIDGE_DATA_ATTACHMENT_CLIENT_SYNC_READY level=53 player=59 session=2`

Prepared artifact evidence:

- bridge SHA-256: `b2e75249f783ea93200cd1374bd21c65b28fb01ed2b6f60307be1f45569ee6bc`
- fixture SHA-256: `56be8e0fb7370692e0d6df018b2021e3ed7dbbd590927129a7e7fbbb225fa06b`
- lock SHA-256: `785298ab9de1241620ea4b8a33d019f2ca2f67a13870ddcbdbe328d5339b766b`

Unit contracts additionally prove persistent-only serialization, registry
lookup, built-in synchronization predicates, null/error semantics, and
copy-on-death filtering. Negotiation contracts encode/decode the client set,
intersect it with server-registered syncable IDs, and reject an unknown
client-only ID. Runtime callbacks transfer attachments on respawn,
entity world replacement, and mob conversion using the completed Entity Events
bridge.

The graphical run proves equal-set negotiation but not a remote client and
server with different mod lists; mismatched-set intersection is currently a
unit contract. This evidence also does not yet prove correctly ordered initial
wire delivery and application for entity, block-entity, and chunk targets,
packet partitioning/limits, unknown-target policy, or a generated
ProtoChunk-to-LevelChunk transfer. Those remain open before Data Attachment API
v1 is called complete. It also does not complete M5 or establish the roadmap's
60% catalog gate.
