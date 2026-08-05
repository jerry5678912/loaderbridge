# Fabric Data Attachment API v1 controlled fixture

This fixture tests the pinned `fabric-data-attachment-api-v1:1.4.7+5b36e0f719`
contract through LoaderBridge revision 9 on Minecraft 1.21.1 and Forge 52.1.16.

The source fixture is compiled as a Fabric mod and transformed by the ordinary
`prepare` command. Content inspection automatically selected Data Attachment
API plus API Base, Entity Events, Lifecycle Events, Networking, and Object
Builder. No module was selected from a filename.

The dedicated-server scenario proves the first storage and persistence wave:

- all four official target families are injected: entity, block entity, level,
  and chunk;
- default initialization, get, set, has, modify, remove, and throw semantics;
- identity-keyed attachment types and codec-backed NBT serialization;
- an actual entity save/load round trip preserving integer value `19`;
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
- the client observed level `53`, player `59`, persistent entity `67`, block
  entity `71`, and chunk `73` in the fresh session;
- only the first session spawned the synchronized passive entity; after save
  and world reopen, session 2 received `67` for that already-loaded entity at
  the packet-order boundary following its spawn bundle;
- before each play session, a configuration task negotiated exactly the five
  syncable fixture attachment IDs and blocked until the response arrived;
- the integrated server simultaneously restored persistent values `41/43` and
  the graphical process stopped cleanly.

The runtime marker was:

`LOADERBRIDGE_DATA_ATTACHMENT_BASE_READY entity=19 block=23 level=8 chunk=31`

`LOADERBRIDGE_DATA_ATTACHMENT_PERSIST_INIT level=41 chunk=43`

`LOADERBRIDGE_DATA_ATTACHMENT_PERSIST_RELOAD level=41 chunk=43`

`LOADERBRIDGE_DATA_ATTACHMENT_TARGET_SYNC_READY level=53 player=59 entity=67 block=71 chunk=73 session=1`

`LOADERBRIDGE_DATA_ATTACHMENT_TARGET_SYNC_READY level=53 player=59 entity=67 block=71 chunk=73 session=2`

Prepared artifact evidence:

- bridge SHA-256: `4729842ba12bd8a196364804832844de185757338d8da93666a33a050dbd7f39`
- fixture SHA-256: `b3287947adecacd6c3e8301bdb4c94e6bcb0912d89ada207d6206b94c5ef5253`
- lock SHA-256: `fb1d8d49f660c5c722505f32fab73c7c3e5016925f33418848dcd1e6b5826457`

Unit contracts additionally prove persistent-only serialization, registry
lookup, built-in synchronization predicates, null/error semantics, and
copy-on-death filtering. Negotiation contracts encode/decode the client set,
intersect it with server-registered syncable IDs, and reject an unknown
client-only ID. Runtime callbacks transfer attachments on respawn,
entity world replacement, and mob conversion using the completed Entity Events
bridge.

Revision 9 unit contracts encode real payloads while packing them, prove every
partition stays within the 1 MiB clientbound limit, cap batches at 4,096
changes, reject an oversized single attachment as `LB-ATTACH-005`, and apply
the default-warning or strict-failure unknown-target policy as `LB-ATTACH-006`.
The two-session graphical scenario passed unchanged with this bounded sender.

The graphical run proves equal-set negotiation but not a remote client and
server with different mod lists; mismatched-set intersection is currently a
unit contract. This evidence does not yet prove a generated
ProtoChunk-to-LevelChunk transfer. That remains open before Data Attachment API
v1 is called complete. It also does not complete M5 or establish the roadmap's
60% catalog gate.
