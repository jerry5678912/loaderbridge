# Fabric Networking API v1 controlled fixture

This fixture tests the pinned `fabric-networking-api-v1:4.3.1+d30f6a7919`
contract through LoaderBridge revision 5 on Minecraft 1.21.1 and Forge 52.1.16.

The source fixture is compiled as a Fabric mod and transformed by the ordinary
`prepare` command. Content inspection selects Networking API and its required
bridge modules; selection does not depend on the fixture filename or mod ID.

The fixture registers `loaderbridge:bidirectional_play` as both C2S and S2C,
using the same Fabric payload type and codec. It independently registers
`loaderbridge:bidirectional_config` as both configuration C2S and S2C. The
bridge assigns four private phase-and-direction-specific Forge wire IDs, then
restores the original Fabric payload and logical ID before invoking handlers.

The graphical Forge scenario proved:

- configuration request and response complete before each play connection;
- Fabric `canSend` exposes the original logical ID rather than the private wire
  ID;
- the server sends a play `ping` and receives the client's play `pong` through
  the same logical payload ID;
- both exchanges pass in the initial world and again after save/reload;
- all dimensions save and the client stops cleanly.

The repeated runtime markers were:

`LOADERBRIDGE_FABRIC_CONFIG_CLIENT_RECEIVED`

`LOADERBRIDGE_FABRIC_CONFIG_SERVER_ROUNDTRIP`

`LOADERBRIDGE_FABRIC_NETWORK_CLIENT_RECEIVED`

`LOADERBRIDGE_FABRIC_NETWORK_SERVER_ROUNDTRIP`

Prepared artifact evidence:

- bridge SHA-256: `6967394fff76c90b70b63747e15fca44118ee082c75551f586b77c00e7fd6149`
- fixture SHA-256: `3e9e8411fff40099f465922b2458eec7e854f6e5e7d1013b5d4aa20621022078`
- lock SHA-256: `8a8f342a08099be452611df2f6aab6fa9aa1265d6d699d9e23563a83ffe641ba`

Unit contracts additionally prove distinct play/configuration and C2S/S2C wire
IDs for the same logical ID, missing-codec diagnostics, duplicate rejection,
receiver registration, and packet-buffer semantics. The full repository build
also exercises Recipe API and Data Attachment negotiation through the new
direction-aware remote-channel queries.

This closes the generic same-ID bidirectional Networking API compatibility
gate. Login-stage networking remains intentionally unadvertised. This evidence
does not complete M5 or establish the roadmap's 60% catalog gate.
