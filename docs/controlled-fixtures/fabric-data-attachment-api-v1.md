# Fabric Data Attachment API v1 controlled fixture

This fixture tests the pinned `fabric-data-attachment-api-v1:1.4.7+5b36e0f719`
contract through LoaderBridge revision 2 on Minecraft 1.21.1 and Forge 52.1.16.

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
- explicit all-dimension flush and clean process exit.

The runtime marker was:

`LOADERBRIDGE_DATA_ATTACHMENT_BASE_READY entity=19 block=23 level=8 chunk=31`

Prepared artifact evidence:

- bridge SHA-256: `3db37bfc0490a3a9f7e26eb0110f1952f71df9ffed86016464ea4656d1e8cea8`
- fixture SHA-256: `18583bd94c49a1cfc36673636fa9bb7843225590197eab47b31b1eb5cc9c9d34`
- lock SHA-256: `a89fabab938a90930d097305129b3f5a17fd5c1b855453d4c15dfbb335dffc79`

Unit contracts additionally prove persistent-only serialization, registry
lookup, built-in synchronization predicates, null/error semantics, and
copy-on-death filtering. Runtime callbacks transfer attachments on respawn,
entity world replacement, and mob conversion using the completed Entity Events
bridge.

This evidence does not yet prove server-level SavedData persistence, chunk
serializer persistence, proto-to-level chunk transfer, client synchronization,
or cross-process persistence. Those are explicit open gates before Data
Attachment API v1 is called complete. It also does not complete M5 or establish
the roadmap's 60% catalog gate.
