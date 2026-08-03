# Fabric Registry Sync v0 bridge

This module targets `fabric-registry-sync-v0` 5.1.3+60c3209b19 for Minecraft
1.21.1. The bridge implements static registry builders and attributes plus
entry-added callbacks and translates Forge native ID-mapping events into
Fabric's immutable old/new remap-state contract. It also implements data-driven dynamic
registry registration, codec loading, and synchronized client transfer. Forge
remains responsible for the actual server/client registry handshake.

Dynamic registry setup callbacks receive a mutable-registry view before datapack
entries load, and `SKIP_WHEN_EMPTY` suppresses empty registry serialization and
tag synchronization. Both behaviors are exercised before initial world entry
and again during world reload in the graphical Forge laboratory.

The remap-state translation has direct contract coverage and a real-client
listener smoke test. A changed-ID, two-version registry scenario remains part
of the broader compatibility laboratory rather than this single-version fixture.
