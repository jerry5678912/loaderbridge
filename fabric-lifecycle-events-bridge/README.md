# Fabric lifecycle events bridge

This module targets the official
`fabric-lifecycle-events-v1:2.6.0+0865547519` binary contract from Fabric API
`0.116.15+1.21.1`. The implementation provides all four server tick callbacks
through Forge's pre/post server and level tick events, plus the complete common
tag-loading callback through Forge's tag-update event.

Authoritative references:

- https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-lifecycle-events-v1/2.6.0%2B0865547519/
- https://github.com/FabricMC/fabric-api/tree/0.116.15%2B1.21.1/fabric-lifecycle-events-v1
- https://github.com/MinecraftForge/MinecraftForge/blob/1.21.x/src/main/java/net/minecraftforge/event/TickEvent.java
- https://github.com/MinecraftForge/MinecraftForge/blob/1.21.x/src/main/java/net/minecraftforge/event/TagsUpdatedEvent.java
