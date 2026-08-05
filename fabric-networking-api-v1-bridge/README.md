# Fabric Networking API v1 bridge

This module independently implements the advertised binary contract
of `fabric-networking-api-v1:4.3.1+d30f6a7919` for Minecraft 1.21.1. Its public
surface is based on Fabric API's Apache-2.0 interfaces. Payload codecs and
handlers are hosted by Forge 52.1.x's payload channel.

Implemented in this release:

- play C2S/S2C payload type registries;
- packet byte-buffer helpers and packet senders;
- global and connection-scoped server play receivers;
- global client play receivers and bidirectional sending;
- server play init, join, and disconnect events;
- server/world/chunk/proximity/entity player lookup helpers;
- entity start/stop tracking callbacks backed by Forge tracking events;
- Fabric's internal untracked-handler marker used by synthetic fake-player
  connections, preventing them from being treated as normal network clients;
- server configuration C2S receivers, S2C sending, channel queries, packet
  senders, and configure/disconnect lifecycle events;
- client configuration S2C receivers, C2S sending, channel queries, packet
  senders, and init/start/complete/disconnect lifecycle events;
- transparent phase-and-direction-specific Forge wire IDs, allowing one
  logical Fabric payload ID to be registered bidirectionally while preserving
  original IDs and payload objects for Fabric handlers and channel queries;
- content-based automatic module selection with stable duplicate, late, and
  missing-codec diagnostics.

Login-stage contracts remain intentionally unadvertised until implemented.
Referencing one of those types therefore fails planning with `LB-FAPI-001`
instead of producing a partially compatible launch.

The controlled Forge 52.1.16 graphical scenario registers the same logical ID
in both directions for play and configuration. Both request/response exchanges
pass before and after world save/reload; see
[`fabric-networking-api-v1.md`](../docs/controlled-fixtures/fabric-networking-api-v1.md).

Reference sources:

- <https://github.com/FabricMC/fabric/tree/d30f6a7/fabric-networking-api-v1>
- <https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-networking-api-v1/4.3.1%2Bd30f6a7919/>
