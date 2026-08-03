# Fabric Registry Sync v0 bridge

This module targets `fabric-registry-sync-v0` 5.1.3+60c3209b19 for Minecraft
1.21.1. The bridge implements static registry builders and attributes plus
entry-added and remap event contracts. It also implements data-driven dynamic
registry registration, codec loading, and synchronized client transfer. Forge
remains responsible for the actual server/client registry handshake.

Dynamic registry setup callbacks and `SKIP_WHEN_EMPTY` packet filtering remain
gated until their mutation and empty-registry connection scenarios pass.
