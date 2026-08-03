# Fabric lifecycle events bridge

This module targets the official
`fabric-lifecycle-events-v1:2.6.0+0865547519` binary contract from Fabric API
`0.116.15+1.21.1`. The implementation now provides the complete
`ServerTickEvents`, `CommonLifecycleEvents`, `ServerLifecycleEvents`, and
`ServerWorldEvents`
classes. Forge events carry server lifecycle, datapack sync, tick, and tag
callbacks. A version-pinned Mixin supplies Fabric's exact `MinecraftServer`
save and datapack-reload hook points before the affected methods return.

Authoritative references:

- https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-lifecycle-events-v1/2.6.0%2B0865547519/
- https://github.com/FabricMC/fabric-api/tree/0.116.15%2B1.21.1/fabric-lifecycle-events-v1
- https://github.com/MinecraftForge/MinecraftForge/blob/1.21.x/src/main/java/net/minecraftforge/event/TickEvent.java
- https://github.com/MinecraftForge/MinecraftForge/blob/1.21.x/src/main/java/net/minecraftforge/event/TagsUpdatedEvent.java
- https://github.com/MinecraftForge/MinecraftForge/blob/1.21.x/src/main/java/net/minecraftforge/event/OnDatapackSyncEvent.java
- https://github.com/MinecraftForge/MinecraftForge/tree/1.21.x/src/main/java/net/minecraftforge/event/server
- https://github.com/MinecraftForge/MinecraftForge/blob/1.21.x/src/main/java/net/minecraftforge/event/level/LevelEvent.java
