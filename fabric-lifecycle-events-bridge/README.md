# Fabric lifecycle events bridge

This module targets the official
`fabric-lifecycle-events-v1:2.6.0+0865547519` binary contract from Fabric API
`0.116.15+1.21.1`. The first implementation release provides all four server
tick callbacks through Forge's pre/post server and level tick events.

Authoritative references:

- https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-lifecycle-events-v1/2.6.0%2B0865547519/
- https://github.com/FabricMC/fabric-api/tree/0.116.15%2B1.21.1/fabric-lifecycle-events-v1
- https://github.com/MinecraftForge/MinecraftForge/blob/1.21.x/src/main/java/net/minecraftforge/event/TickEvent.java
