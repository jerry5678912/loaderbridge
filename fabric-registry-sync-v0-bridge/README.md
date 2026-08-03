# Fabric Registry Sync v0 bridge

This module targets `fabric-registry-sync-v0` 5.1.3+60c3209b19 for Minecraft
1.21.1. The first slice implements static registry builders and attributes plus
entry-added and remap event contracts. Forge remains responsible for the actual
server/client registry handshake.

Dynamic data-driven registry registration and its setup callback remain gated
until their codec, datapack, and connection scenarios pass.
