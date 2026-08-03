# Fabric Networking API v1 bridge

This module independently implements the implemented play-stage binary contract
of `fabric-networking-api-v1:4.3.1+d30f6a7919` for Minecraft 1.21.1. Its public
surface is based on Fabric API's Apache-2.0 interfaces. Payload codecs and
handlers are hosted by Forge 52.1.x's payload channel.

Implemented in this release:

- play C2S/S2C payload type registries;
- packet byte-buffer helpers and packet senders;
- global and connection-scoped server play receivers;
- global client play receivers and bidirectional sending;
- server play init, join, and disconnect events;
- content-based automatic module selection with stable duplicate, late, and
  missing-codec diagnostics.

Configuration-stage, login-stage, tracking, and player-lookup contracts remain
intentionally unadvertised until implemented. Referencing one of those types
therefore fails planning with `LB-FAPI-001` instead of producing a partially
compatible launch.

Reference sources:

- <https://github.com/FabricMC/fabric-api/tree/0.116.15%2B1.21.1/fabric-networking-api-v1>
- <https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-networking-api-v1/4.3.1%2Bd30f6a7919/>
