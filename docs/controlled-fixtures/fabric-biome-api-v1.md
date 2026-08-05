# Fabric Biome API v1 controlled fixture

This fixture tests LoaderBridge revision 2 of pinned
`fabric-biome-api-v1:13.0.31+d527f9fd19` on Minecraft 1.21.1 and Forge
52.1.16. The ordinary `prepare` command inspects bytecode and automatically
selects the Biome bridge and its dependencies without relying on artifact
filenames or mod IDs.

The translated Fabric lifecycle fixture registers a custom monster entity and
uses the public Fabric contracts to add two entries to Plains:

- `Carvers.NETHER_CAVE` in the AIR carving step;
- the custom monster with weight 3 and a group range of 1 to 2.

The runtime assertion reads the live Plains biome after server startup and
requires both additions. It emits:

`LOADERBRIDGE_FABRIC_BIOME_ADDITIONS_READY spawn=1,carver=nether`

The marker appeared during both halves of a graphical world creation, save,
and reload scenario. It also appeared in two independent dedicated-server
processes: the first generated a new world and the second reloaded it. Every
process saved the overworld, Nether, and End and stopped cleanly.

Repeated preparation produced byte-identical artifacts:

- bridge SHA-256: `409a9e2a514bed848002ea7600903a1b2f9610282539c0d72f11da56f2eec0d3`
- transformed fixture SHA-256: `192729144903480a733e025bf9469935e52218af69ba77b12f42944d7a28010b`

The bridge emits `LB-BIOME-003` when a configured-carver target cannot be
resolved. This revision covers selector-driven feature, carver, and spawn
additions. The general phased modification context, removals, climate and
effects mutation, and specialized Nether/End helpers remain open; this does not
complete the Biome API module, M5, or the roadmap's 60% catalog gate.
